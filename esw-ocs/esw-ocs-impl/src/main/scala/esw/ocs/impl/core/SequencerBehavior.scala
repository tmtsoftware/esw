package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{QueryFinal, SubmitSequenceAndWait}
import csw.command.client.messages.{GetComponentLogMetadata, LogControlMessage, SetComponentLogLevel}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.logging.client.commons.LogAdminUtil
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.codecs.OcsCodecs
import esw.ocs.api.protocol._
import esw.ocs.dsl.script.JScriptDsl
import esw.ocs.impl.internal.Timeouts
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState
import esw.ocs.impl.messages.SequencerState._

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class SequencerBehavior(
    componentId: ComponentId,
    script: JScriptDsl,
    locationService: LocationService,
    shutdownHttpService: () => Future[Done]
)(implicit val actorSystem: ActorSystem[_])
    extends OcsCodecs {
  import actorSystem.executionContext

  def setup: Behavior[SequencerMsg] = Behaviors.setup { ctx =>
    idle(SequencerData.initial(ctx.self))
  }

  //BEHAVIORS
  private def idle(data: SequencerData): Behavior[SequencerMsg] = receive(Idle, data, idle) {
    case LoadSequence(sequence, replyTo)                  => load(sequence, replyTo, data)
    case SubmitSequenceAndWaitInternal(sequence, replyTo) => submitSequenceAndWait(sequence, data, replyTo)
    case SubmitSequence(sequence, replyTo)                => submitSequence(sequence, data, replyTo)
    case QueryFinalInternal(replyTo)                      => idle(data.queryFinal(replyTo))
    case GoOffline(replyTo)                               => goOffline(replyTo, data)(idle)
    case PullNext(replyTo)                                => idle(data.pullNextStep(replyTo))
  }

  private def loaded(data: SequencerData): Behavior[SequencerMsg] = receive(Loaded, data, loaded) {
    case QueryFinalInternal(replyTo)     => loaded(data.queryFinal(replyTo))
    case msg: EditorAction               => handleEditorAction(msg, data, Loaded)(nextBehavior = loaded)
    case GoOffline(replyTo)              => goOffline(replyTo, data)(loaded)
    case StartSequence(replyTo)          => inProgress(data.startSequence(replyTo))
    case LoadSequence(sequence, replyTo) => load(sequence, replyTo, data)
  }

  private def inProgress(data: SequencerData): Behavior[SequencerMsg] = receive(InProgress, data, inProgress) {
    case QueryFinalInternal(replyTo) => inProgress(data.queryFinal(replyTo))
    case AbortSequence(replyTo)      => abortSequence(data, InProgress, replyTo)(nextBehavior = inProgress)
    case Stop(replyTo)               => stop(data, InProgress, replyTo)(nextBehavior = inProgress)
    case msg: EditorAction           => handleEditorAction(msg, data, InProgress)(nextBehavior = inProgress)
    case Pause(replyTo)              => inProgress(data.updateStepListResult(replyTo, InProgress, data.stepList.map(_.pause)))
    case Resume(replyTo)             => inProgress(data.updateStepList(replyTo, InProgress, data.stepList.map(_.resume)))
    case PullNext(replyTo)           => inProgress(data.pullNextStep(replyTo))
    case StepSuccess(_)              => inProgress(data.stepSuccess(InProgress))
    case StepFailure(reason, _)      => inProgress(data.stepFailure(reason, InProgress))
    case _: GoIdle                   => idle(data)
  }

  private def offline(data: SequencerData): Behavior[SequencerMsg] = receive(Offline, data, offline) {
    case GoOnline(replyTo) => goOnline(replyTo, data)
  }

  private def shuttingDown(data: SequencerData): Behavior[SequencerMsg] = receive(ShuttingDown, data, shuttingDown) {
    case ShutdownComplete(replyTo) =>
      replyTo ! Ok
      actorSystem.terminate()
      Behaviors.stopped
  }

  private def goingOnline(data: SequencerData): Behavior[SequencerMsg] =
    receive(GoingOnline, data, goingOnline) {
      case GoOnlineSuccess(replyTo) => replyTo ! Ok; idle(data)
      case GoOnlineFailed(replyTo)  => replyTo ! GoOnlineHookFailed; offline(data)
    }

  private def goingOffline(data: SequencerData)(
      currentBehavior: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = receive(GoingOffline, data, currentBehavior) {
    case GoOfflineSuccess(replyTo) => replyTo ! Ok; offline(data.copy(stepList = None))
    case GoOfflineFailed(replyTo)  => replyTo ! GoOfflineHookFailed; currentBehavior(data)
  }

  private def abortingSequence(
      data: SequencerData,
      state: SequencerState[SequencerMsg]
  )(nextBehavior: SequencerData => Behavior[SequencerMsg]): Behavior[SequencerMsg] =
    receive[AbortSequenceMessage](AbortingSequence, data, abortingSequence(_, state)(nextBehavior)) {
      case AbortSequenceComplete(replyTo) =>
        import data._
        val maybeStepList = stepList.map(_.discardPending)
        nextBehavior(updateStepList(replyTo, state, maybeStepList))
    }

  private def stopping(
      data: SequencerData,
      state: SequencerState[SequencerMsg]
  )(nextBehavior: SequencerData => Behavior[SequencerMsg]): Behavior[SequencerMsg] =
    receive[StopMessage](Stopping, data, stopping(_, state)(nextBehavior)) {
      case StopComplete(replyTo) =>
        import data._
        nextBehavior(updateStepList(replyTo, state, stepList))
    }

  private def handleCommonMessage[T <: SequencerMsg](
      message: CommonMessage,
      state: SequencerState[T],
      data: SequencerData,
      currentBehavior: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = message match {
    case Shutdown(replyTo)                        => shutdown(data, replyTo)
    case GetSequence(replyTo)                     => replyTo ! data.stepList; Behaviors.same
    case GetSequencerState(replyTo)               => replyTo ! state; Behaviors.same
    case DiagnosticMode(startTime, hint, replyTo) => goToDiagnosticMode(startTime, hint, replyTo)
    case OperationsMode(replyTo)                  => goToOperationsMode(replyTo)
    case ReadyToExecuteNext(replyTo)              => currentBehavior(data.readyToExecuteNext(replyTo, state))
    case MaybeNext(replyTo) =>
      if (state == InProgress) replyTo ! data.stepList.flatMap(_.nextExecutable)
      else replyTo ! None
      Behaviors.same
  }

  private def handleEditorAction(editorAction: EditorAction, data: SequencerData, state: SequencerState[SequencerMsg])(
      nextBehavior: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    import data._
    editorAction match {
      case Add(commands, replyTo)            => nextBehavior(updateStepList(replyTo, state, stepList.map(_.append(commands))))
      case Prepend(commands, replyTo)        => nextBehavior(updateStepList(replyTo, state, stepList.map(_.prepend(commands))))
      case Delete(id, replyTo)               => nextBehavior(updateStepListResult(replyTo, state, stepList.map(_.delete(id))))
      case Reset(replyTo) if state == Loaded => idle(updateStepList(replyTo, state, stepList = None))
      case Reset(replyTo)                    => nextBehavior(updateStepList(replyTo, state, stepList.map(_.discardPending)))
      case Replace(id, commands, replyTo) =>
        nextBehavior(updateStepListResult(replyTo, state, stepList.map(_.replace(id, commands))))
      case InsertAfter(id, commands, replyTo) =>
        nextBehavior(updateStepListResult(replyTo, state, stepList.map(_.insertAfter(id, commands))))
      case AddBreakpoint(id, replyTo) => nextBehavior(updateStepListResult(replyTo, state, stepList.map(_.addBreakpoint(id))))
      case RemoveBreakpoint(id, replyTo) =>
        nextBehavior(updateStepListResult(replyTo, state, stepList.map(_.removeBreakpoint(id))))
    }
  }

  private def abortSequence(data: SequencerData, state: SequencerState[SequencerMsg], replyTo: ActorRef[OkOrUnhandledResponse])(
      nextBehavior: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    script.executeAbort().onComplete(_ => data.self ! AbortSequenceComplete(replyTo))
    abortingSequence(data, state)(nextBehavior)
  }

  private def stop(data: SequencerData, state: SequencerState[SequencerMsg], replyTo: ActorRef[OkOrUnhandledResponse])(
      nextBehavior: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    script.executeStop().onComplete(_ => data.self ! StopComplete(replyTo))
    stopping(data, state)(nextBehavior)
  }

  private def load(sequence: Sequence, replyTo: ActorRef[OkOrUnhandledResponse], data: SequencerData): Behavior[SequencerMsg] = {
    replyTo ! Ok
    loaded(data.createStepList(sequence))
  }

  // fixme: Ok is sent twice to replyTo
  private def submitSequence(
      sequence: Sequence,
      data: SequencerData,
      replyTo: ActorRef[OkOrUnhandledResponse]
  ): Behavior[SequencerMsg] = {
    replyTo ! Ok
    inProgress(
      data
        .createStepList(sequence)
        .startSequence(replyTo)
    )
  }

  private def submitSequenceAndWait(
      sequence: Sequence,
      data: SequencerData,
      replyTo: ActorRef[SequenceResponse]
  ): Behavior[SequencerMsg] = {
    inProgress(
      data
        .createStepList(sequence)
        .queryFinal(replyTo)
        .processSequence()
    )
  }

  private def shutdown(data: SequencerData, replyTo: ActorRef[Ok.type]): Behavior[SequencerMsg] = {

    // run both the futures in parallel and wait for both to complete
    // once all finished, send ShutdownComplete self message irrespective of any failures
    val f1 = locationService.unregister(AkkaConnection(componentId))
    val f2 = script.executeShutdown()
    val f3 = shutdownHttpService()
    f1.onComplete(_ => f2.onComplete(_ => f3.onComplete(_ => data.self ! ShutdownComplete(replyTo))))

    shuttingDown(data)
  }

  private def goOnline(replyTo: ActorRef[GoOnlineResponse], data: SequencerData): Behavior[SequencerMsg] = {
    script.executeGoOnline().onComplete {
      case Success(_) => data.self ! GoOnlineSuccess(replyTo)
      case Failure(_) => data.self ! GoOnlineFailed(replyTo)
    }
    goingOnline(data)
  }

  private def goOffline(replyTo: ActorRef[GoOfflineResponse], data: SequencerData)(
      currentBehavior: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    // go to offline state even if handler fails, note that this is different than GoOnline
    script.executeGoOffline().onComplete {
      case Success(_) => data.self ! GoOfflineSuccess(replyTo)
      case Failure(_) => data.self ! GoOfflineFailed(replyTo)
    }
    goingOffline(data)(currentBehavior)
  }

  private def goToDiagnosticMode(
      startTime: UTCTime,
      hint: String,
      replyTo: ActorRef[DiagnosticModeResponse]
  ): Behavior[SequencerMsg] = {
    script.executeDiagnosticMode(startTime, hint).onComplete {
      case Success(_) => replyTo ! Ok
      case _          => replyTo ! DiagnosticHookFailed
    }
    Behaviors.same
  }

  private def goToOperationsMode(replyTo: ActorRef[OperationsModeResponse]): Behavior[SequencerMsg] = {
    script.executeOperationsMode().onComplete {
      case Success(_) => replyTo ! Ok
      case _          => replyTo ! OperationsHookFailed
    }
    Behaviors.same
  }

  private def handleLogMessages(
      msg: LogControlMessage
  ): Behavior[SequencerMsg] = msg match {
    case GetComponentLogMetadata(componentName, replyTo) => replyTo ! LogAdminUtil.getLogMetadata(componentName); Behaviors.same
    case SetComponentLogLevel(componentName, logLevel) =>
      LogAdminUtil.setComponentLogLevel(componentName, logLevel); Behaviors.same
  }

  protected def receive[T <: SequencerMsg: ClassTag](
      state: SequencerState[T],
      data: SequencerData,
      currentBehavior: SequencerData => Behavior[SequencerMsg]
  )(f: T => Behavior[SequencerMsg]): Behavior[SequencerMsg] =
    Behaviors.receive { (ctx, msg) =>
      implicit val timeout: Timeout = Timeouts.LongTimeout

      msg match {
        case msg: CommonMessage     => handleCommonMessage(msg, state, data, currentBehavior)
        case msg: LogControlMessage => handleLogMessages(msg)
        case msg: T                 => f(msg)
        case msg: UnhandleableSequencerMessage =>
          msg.replyTo ! Unhandled(state.entryName, msg.getClass.getSimpleName); Behaviors.same
        case SubmitSequenceAndWait(sequence, replyTo) =>
          val sequenceResponseF: Future[SequenceResponse] = ctx.self ? (SubmitSequenceAndWaitInternal(sequence, _))
          sequenceResponseF.foreach(res => replyTo ! res.toSubmitResponse(sequence.runId))
          Behaviors.same
        case QueryFinal(replyTo) =>
          (ctx.self ? QueryFinalInternal).foreach(res => replyTo ! res.toSubmitResponse())
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }
}
