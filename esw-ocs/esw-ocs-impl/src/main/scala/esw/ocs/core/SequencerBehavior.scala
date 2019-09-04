package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.{LoadAndProcessSequence, SequencerMsg}
import csw.command.client.messages.{GetComponentLogMetadata, LogControlMessages, SetComponentLogLevel}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.logging.client.commons.LogAdminUtil
import csw.params.commands.Sequence
import esw.ocs.api.models.codecs.OcsCodecs
import esw.ocs.api.models.responses.{GoOnlineHookFailed, _}
import esw.ocs.client.messages.SequencerMessages._
import esw.ocs.client.messages.SequencerState
import esw.ocs.client.messages.SequencerState._
import esw.ocs.dsl.ScriptDsl
import esw.ocs.internal.Timeouts

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class SequencerBehavior(
    componentId: ComponentId,
    script: ScriptDsl,
    locationService: LocationService,
    crm: CommandResponseManager
)(implicit val actorSystem: ActorSystem[_], timeout: Timeout)
    extends OcsCodecs {
  import actorSystem.executionContext

  def setup: Behavior[SequencerMsg] = Behaviors.setup { ctx =>
    idle(SequencerData.initial(ctx.self, crm))
  }

  //BEHAVIORS
  private def idle(data: SequencerData): Behavior[SequencerMsg] = receive(Idle, data, idle) {
    case LoadSequence(sequence, replyTo)                   => load(sequence, replyTo, data)
    case LoadAndProcessSequenceInternal(sequence, replyTo) => loadAndProcess(sequence, data, replyTo)
    case LoadAndStartSequence(sequence, replyTo)           => loadAndStart(sequence, data, replyTo)
    case QuerySequenceResponse(replyTo)                    => idle(data.querySequence(replyTo))
    case GoOffline(replyTo)                                => goOffline(replyTo, data)
    case PullNext(replyTo)                                 => idle(data.pullNextStep(replyTo))
  }

  private def loaded(data: SequencerData): Behavior[SequencerMsg] = receive(Loaded, data, loaded) {
    case QuerySequenceResponse(replyTo) => loaded(data.querySequence(replyTo))
    case AbortSequence(replyTo)         => abortSequence(data, Loaded, replyTo)(nextBehavior = idle)
    case editorAction: EditorAction     => handleEditorAction(editorAction, data, Loaded)(nextBehavior = loaded)
    case GoOffline(replyTo)             => goOffline(replyTo, data)
    case StartSequence(replyTo)         => inProgress(data.startSequence(replyTo))
  }

  private def inProgress(data: SequencerData): Behavior[SequencerMsg] = receive(InProgress, data, inProgress) {
    case QuerySequenceResponse(replyTo) => inProgress(data.querySequence(replyTo))
    case AbortSequence(replyTo)         => abortSequence(data, InProgress, replyTo)(nextBehavior = inProgress)
    case msg: EditorAction              => handleEditorAction(msg, data, InProgress)(nextBehavior = inProgress)
    case PullNext(replyTo)              => inProgress(data.pullNextStep(replyTo))
    case Update(submitResponse, _)      => inProgress(data.updateStepStatus(submitResponse, InProgress))
    case _: GoIdle                      => idle(data)
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

  private def goingOffline(data: SequencerData): Behavior[SequencerMsg] = receive(GoingOffline, data, goingOffline) {
    case GoneOffline(replyTo) => replyTo ! Ok; offline(data.copy(stepList = None))
  }

  private def abortingSequence(
      data: SequencerData,
      state: SequencerState[SequencerMsg]
  )(nextBehavior: SequencerData => Behavior[SequencerMsg]): Behavior[SequencerMsg] =
    receive[AbortSequenceMessage](AbortingSequence, data, abortingSequence(_, state)(nextBehavior)) {
      case AbortSequenceComplete(replyTo) =>
        import data._
        val maybeStepList = stepList.flatMap { x =>
          val inProgressStepList = x.discardPending
          if (inProgressStepList.steps.isEmpty) None
          else Some(inProgressStepList)
        }
        nextBehavior(updateStepList(replyTo, state, maybeStepList))
    }

  private def handleCommonMessage[T <: SequencerMsg](
      message: CommonMessage,
      state: SequencerState[T],
      data: SequencerData,
      currentBehavior: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = message match {
    case Shutdown(replyTo)           => shutdown(data, replyTo)
    case GetSequence(replyTo)        => replyTo ! data.stepList; Behaviors.same
    case GetSequencerState(replyTo)  => replyTo ! state; Behaviors.same
    case ReadyToExecuteNext(replyTo) => currentBehavior(data.readyToExecuteNext(replyTo, state))
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
      case Pause(replyTo)                    => nextBehavior(updateStepListResult(replyTo, state, stepList.map(_.pause)))
      case Resume(replyTo)                   => nextBehavior(updateStepList(replyTo, state, stepList.map(_.resume)))
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

  def createStepList(sequence: Sequence, data: SequencerData, replyTo: ActorRef[DuplicateIdsFound.type])(
      onSuccess: SequencerData => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = data.createStepList(sequence) match {
    case Left(err)          => replyTo ! err; Behaviors.same
    case Right(updatedData) => onSuccess(updatedData)
  }

  private def load(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse], data: SequencerData): Behavior[SequencerMsg] =
    createStepList(sequence, data, replyTo) { updatedData =>
      replyTo ! Ok
      loaded(updatedData)
    }

  private def loadAndStart(
      sequence: Sequence,
      data: SequencerData,
      replyTo: ActorRef[LoadSequenceResponse]
  ): Behavior[SequencerMsg] = createStepList(sequence, data, replyTo) { updatedData =>
    replyTo ! Ok
    inProgress(updatedData.startSequence(replyTo))
  }

  private def loadAndProcess(
      sequence: Sequence,
      data: SequencerData,
      replyTo: ActorRef[SequenceResponse]
  ): Behavior[SequencerMsg] =
    createStepList(sequence, data, replyTo) { updatedData =>
      inProgress(updatedData.processSequence { res =>
        replyTo ! SequenceResult(res)
        updatedData.goToIdle()
      })
    }

  private def shutdown(data: SequencerData, replyTo: ActorRef[Ok.type]): Behavior[SequencerMsg] = {

    // run both the futures in parallel and wait for both to complete
    // once all finished, send ShutdownComplete self message irrespective of any failures
    val f1 = locationService.unregister(AkkaConnection(componentId))
    val f2 = script.executeShutdown()
    f1.onComplete(_ => f2.onComplete(_ => data.self ! ShutdownComplete(replyTo)))

    shuttingDown(data)
  }

  private def goOnline(replyTo: ActorRef[GoOnlineResponse], data: SequencerData): Behavior[SequencerMsg] = {
    script.executeGoOnline().onComplete {
      case Success(_) => data.self ! GoOnlineSuccess(replyTo)
      case Failure(_) => data.self ! GoOnlineFailed(replyTo)
    }
    goingOnline(data)
  }

  private def goOffline(replyTo: ActorRef[OkOrUnhandledResponse], data: SequencerData): Behavior[SequencerMsg] = {
    // go to offline state even if handler fails, note that this is different than GoOnline
    script.executeGoOffline().onComplete(_ => data.self ! GoneOffline(replyTo))
    goingOffline(data)
  }

  private def handleLogMessages(
      msg: LogControlMessages
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
      implicit val timeout: Timeout     = Timeouts.LongTimeout
      implicit val scheduler: Scheduler = ctx.system.scheduler

      msg match {
        case msg: CommonMessage      => handleCommonMessage(msg, state, data, currentBehavior)
        case msg: LogControlMessages => handleLogMessages(msg)
        case msg: T                  => f(msg)
        case msg: UnhandleableSequencerMessage =>
          msg.replyTo ! Unhandled(state.entryName, msg.getClass.getSimpleName); Behaviors.same
        case LoadAndProcessSequence(sequence, replyTo) =>
          val sequenceResponseF: Future[SequenceResponse] = ctx.self ? (LoadAndProcessSequenceInternal(sequence, _))
          sequenceResponseF.foreach(res => replyTo ! res.toSubmitResponse(sequence.runId))
          Behaviors.same
        case _ => Behaviors.unhandled
      }
    }
}
