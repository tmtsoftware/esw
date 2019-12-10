package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.command.client.messages.{GetComponentLogMetadata, LogControlMessage, SetComponentLogLevel}
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId}
import csw.logging.client.commons.LogAdminUtil
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.codecs.OcsCodecs
import esw.ocs.api.protocol._
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.impl.internal.Timeouts
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState
import esw.ocs.impl.messages.SequencerState._

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class SequencerBehavior(
    componentId: ComponentId,
    script: ScriptDsl,
    locationService: LocationService,
    sequenceComponentLocation: AkkaLocation,
    shutdownHttpService: () => Future[Done]
)(implicit val actorSystem: ActorSystem[_])
    extends OcsCodecs {
  import actorSystem.executionContext

  private def stateMachine(state: SequencerState[_]): SequencerData => Behavior[SequencerMsg] = state match {
    case Idle             => idle
    case Loaded           => loaded
    case InProgress       => inProgress
    case Offline          => offline
    case GoingOnline      => goingOnline
    case GoingOffline     => goingOffline
    case ShuttingDown     => shuttingDown
    case AbortingSequence => abortingSequence
    case Stopping         => stopping
  }

  def setup: Behavior[SequencerMsg] = Behaviors.setup { ctx =>
    idle(SequencerData.initial(ctx.self))
  }

  //BEHAVIORS
  private def idle(data: SequencerData): Behavior[SequencerMsg] = receive(Idle, data) {
    case LoadSequence(sequence, replyTo)           => load(sequence, replyTo, data)
    case SubmitSequenceInternal(sequence, replyTo) => submitSequence(sequence, data, replyTo)
    case GoOffline(replyTo)                        => goOffline(replyTo, data)
    case PullNext(replyTo)                         => idle(data.pullNextStep(replyTo))
  }

  private def loaded(data: SequencerData): Behavior[SequencerMsg] = receive(Loaded, data) {
    case msg: EditorAction               => handleEditorAction(msg, data, Loaded)
    case GoOffline(replyTo)              => goOffline(replyTo, data)
    case StartSequence(replyTo)          => inProgress(data.startSequence(replyTo))
    case LoadSequence(sequence, replyTo) => load(sequence, replyTo, data)
  }

  private def inProgress(data: SequencerData): Behavior[SequencerMsg] = receive(InProgress, data) {
    case AbortSequence(replyTo) => abortSequence(data, replyTo)
    case Stop(replyTo)          => stop(data, replyTo)
    case msg: EditorAction      => handleEditorAction(msg, data, InProgress)
    case Pause(replyTo)         => inProgress(data.updateStepListResult(replyTo, data.stepList.map(_.pause)))
    case Resume(replyTo)        => inProgress(data.updateStepList(replyTo, data.stepList.map(_.resume)))
    case PullNext(replyTo)      => inProgress(data.pullNextStep(replyTo))
    case StepSuccess(_)         => inProgress(data.stepSuccess(InProgress))
    case StepFailure(reason, _) => inProgress(data.stepFailure(reason, InProgress))
    case _: GoIdle              => idle(data)
  }

  private def offline(data: SequencerData): Behavior[SequencerMsg] = receive(Offline, data) {
    case GoOnline(replyTo) => goOnline(replyTo, data)
  }

  private def handleCommonMessage[T <: SequencerMsg](
      message: CommonMessage,
      state: SequencerState[T],
      data: SequencerData
  ): Behavior[SequencerMsg] = message match {
    case Shutdown(replyTo)                        => shutdown(data, replyTo)
    case GetSequence(replyTo)                     => replyTo ! data.stepList; Behaviors.same
    case GetSequencerState(replyTo)               => replyTo ! state; Behaviors.same
    case DiagnosticMode(startTime, hint, replyTo) => goToDiagnosticMode(startTime, hint, replyTo)
    case OperationsMode(replyTo)                  => goToOperationsMode(replyTo)
    case GetSequenceComponent(replyTo)            => replyTo ! sequenceComponentLocation; Behaviors.same
    case ReadyToExecuteNext(replyTo)              => stateMachine(state)(data.readyToExecuteNext(replyTo))
    case MaybeNext(replyTo) =>
      if (state == InProgress) replyTo ! data.stepList.flatMap(_.nextExecutable)
      else replyTo ! None
      Behaviors.same
  }

  // handles all the sequence editor messages. These messages are supported only in Loaded and InProgress state,
  // nextBehavior represents the state (Loaded/InProgress) where it gets called from.
  private def handleEditorAction(
      editorAction: EditorAction,
      data: SequencerData,
      state: SequencerState[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    import data._
    val currentBehavior = stateMachine(state)
    editorAction match {
      case Add(commands, replyTo)            => currentBehavior(updateStepList(replyTo, stepList.map(_.append(commands))))
      case Prepend(commands, replyTo)        => currentBehavior(updateStepList(replyTo, stepList.map(_.prepend(commands))))
      case Delete(id, replyTo)               => currentBehavior(updateStepListResult(replyTo, stepList.map(_.delete(id))))
      case Reset(replyTo) if state == Loaded => idle(updateStepList(replyTo, stepList = None))
      case Reset(replyTo)                    => currentBehavior(updateStepList(replyTo, stepList.map(_.discardPending)))
      case Replace(id, commands, replyTo) =>
        currentBehavior(updateStepListResult(replyTo, stepList.map(_.replace(id, commands))))
      case InsertAfter(id, commands, replyTo) =>
        currentBehavior(updateStepListResult(replyTo, stepList.map(_.insertAfter(id, commands))))
      case AddBreakpoint(id, replyTo) => currentBehavior(updateStepListResult(replyTo, stepList.map(_.addBreakpoint(id))))
      case RemoveBreakpoint(id, replyTo) =>
        currentBehavior(updateStepListResult(replyTo, stepList.map(_.removeBreakpoint(id))))
    }
  }

  private def handleLogMessages(msg: LogControlMessage): Behavior[SequencerMsg] = msg match {
    case GetComponentLogMetadata(componentName, replyTo) => replyTo ! LogAdminUtil.getLogMetadata(componentName); Behaviors.same
    case SetComponentLogLevel(componentName, logLevel) =>
      LogAdminUtil.setComponentLogLevel(componentName, logLevel); Behaviors.same
  }

  private def goOnline(replyTo: ActorRef[GoOnlineResponse], data: SequencerData): Behavior[SequencerMsg] = {
    script.executeGoOnline().onComplete {
      case Success(_) => data.self ! GoOnlineSuccess(replyTo)
      case Failure(_) => data.self ! GoOnlineFailed(replyTo)
    }
    goingOnline(data)
  }

  private def goingOnline(data: SequencerData): Behavior[SequencerMsg] = receive(GoingOnline, data) {
    case GoOnlineSuccess(replyTo) => replyTo ! Ok; idle(data)
    case GoOnlineFailed(replyTo)  => replyTo ! GoOnlineHookFailed; offline(data)
  }

  private def goOffline(replyTo: ActorRef[GoOfflineResponse], data: SequencerData): Behavior[SequencerMsg] = {
    script.executeGoOffline().onComplete {
      case Success(_) => data.self ! GoOfflineSuccess(replyTo)
      case Failure(_) => data.self ! GoOfflineFailed(replyTo)
    }
    goingOffline(data)
  }

  private def goingOffline(data: SequencerData): Behavior[SequencerMsg] = receive(GoingOffline, data) {
    case GoOfflineSuccess(replyTo) => replyTo ! Ok; offline(data.copy(stepList = None))
    case GoOfflineFailed(replyTo) =>
      val currentBehavior: SequencerData => Behavior[SequencerMsg] = if (data.isSequenceLoaded) loaded else idle
      replyTo ! GoOfflineHookFailed; currentBehavior(data)
  }

  private def abortSequence(data: SequencerData, replyTo: ActorRef[OkOrUnhandledResponse]): Behavior[SequencerMsg] = {
    script.executeAbort().onComplete(_ => data.self ! AbortSequenceComplete(replyTo))
    abortingSequence(data)
  }

  private def abortingSequence(data: SequencerData): Behavior[SequencerMsg] = receive(AbortingSequence, data) {
    case AbortSequenceComplete(replyTo) =>
      import data._
      val maybeStepList = stepList.map(_.discardPending)
      inProgress(updateStepList(replyTo, maybeStepList))
  }

  private def stop(data: SequencerData, replyTo: ActorRef[OkOrUnhandledResponse]): Behavior[SequencerMsg] = {
    script.executeStop().onComplete(_ => data.self ! StopComplete(replyTo))
    stopping(data)
  }

  private def stopping(data: SequencerData): Behavior[SequencerMsg] = receive[StopMessage](Stopping, data) {
    case StopComplete(replyTo) =>
      import data._
      val maybeStepList = stepList.map(_.discardPending)
      inProgress(updateStepList(replyTo, maybeStepList))
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

  private def shuttingDown(data: SequencerData): Behavior[SequencerMsg] = receive(ShuttingDown, data) {
    case ShutdownComplete(replyTo) =>
      replyTo ! Ok
      actorSystem.terminate()
      Behaviors.stopped
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

  private def load(sequence: Sequence, replyTo: ActorRef[OkOrUnhandledResponse], data: SequencerData): Behavior[SequencerMsg] = {
    replyTo ! Ok
    loaded(data.createStepList(sequence))
  }

  private def submitSequence(
      sequence: Sequence,
      data: SequencerData,
      replyTo: ActorRef[SequencerSubmitResponse]
  ): Behavior[SequencerMsg] = inProgress(data.createStepList(sequence).startSequence(replyTo))

  private def receive[StateMessage <: SequencerMsg: ClassTag](
      state: SequencerState[StateMessage],
      data: SequencerData
  )(stateHandler: StateMessage => Behavior[SequencerMsg]): Behavior[SequencerMsg] =
    Behaviors.receive { (ctx, msg) =>
      implicit val timeout: Timeout = Timeouts.LongTimeout

      msg match {
        case msg: CommonMessage     => handleCommonMessage(msg, state, data)
        case msg: LogControlMessage => handleLogMessages(msg)
        case msg: StateMessage      => stateHandler(msg)
        case msg: UnhandleableSequencerMessage =>
          msg.replyTo ! Unhandled(state.entryName, msg.getClass.getSimpleName); Behaviors.same

        // SubmitSequence is a CSW SequencerMsg, to be able to handle it only in Idle State, a corresponding Internal message is created
        // and handled in Idle State. If this message is received in any other state, and Unhandled response is returned which is
        // then adapted to a SubmitResponse
        case SubmitSequence(sequence, replyTo) =>
          val submitResponse: Future[SequencerSubmitResponse] = ctx.self ? (SubmitSequenceInternal(sequence, _))
          submitResponse.foreach(res => replyTo ! res.toSubmitResponse())
          Behaviors.same

        case Query(runId, replyTo) => data.query(runId, replyTo); Behaviors.same
        // Behaviors.same is not used below, because new SequencerData (updated with subscribers) needs to passed to currentBehavior
        case QueryFinal(runId, replyTo) => stateMachine(state)(data.queryFinal(runId, replyTo))

        case _ => Behaviors.unhandled
      }
    }
}
