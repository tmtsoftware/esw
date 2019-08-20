package esw.ocs.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.params.commands.Sequence
import esw.ocs.api.codecs.OcsCodecs
import esw.ocs.api.models.SequencerState._
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.{GoOnlineHookFailed, _}
import esw.ocs.dsl.ScriptDsl

import scala.util.{Failure, Success}

class SequencerBehavior(
    componentId: ComponentId,
    script: ScriptDsl,
    locationService: LocationService,
    crm: CommandResponseManager
)(implicit val actorSystem: ActorSystem[_], timeout: Timeout)
    extends CustomReceive
    with OcsCodecs {

  import actorSystem.executionContext

  def setup: Behavior[SequencerMsg] = Behaviors.setup { ctx =>
    idle(SequencerActorState.initial(ctx.self, crm))
  }

  //BEHAVIORS
  private def idle(state: SequencerActorState): Behavior[SequencerMsg] = receive(Idle) {
    case msg: CommonMessage                              => handleCommonMessage(msg, state)
    case LoadSequence(sequence, replyTo)                 => load(sequence, replyTo, state)(nextBehavior = loaded)
    case LoadAndStartSequenceInternal(sequence, replyTo) => loadAndStart(sequence, state, replyTo)
    case GoOffline(replyTo)                              => goOffline(replyTo, state)
    case PullNext(replyTo)                               => idle(state.pullNextStep(replyTo))
  }

  private def loaded(state: SequencerActorState): Behavior[SequencerMsg] = receive(Loaded) {
    case msg: CommonMessage         => handleCommonMessage(msg, state)
    case AbortSequence(replyTo)     => abortSequence(state, replyTo)(nextBehavior = idle)
    case editorAction: EditorAction => loaded(handleEditorAction(editorAction, state))
    case GoOffline(replyTo)         => goOffline(replyTo, state)
    case StartSequence(replyTo)     => start(state, replyTo)
  }

  private def inProgress(state: SequencerActorState): Behavior[SequencerMsg] = receive(InProgress) {
    case msg: CommonMessage          => handleCommonMessage(msg, state)
    case AbortSequence(replyTo)      => abortSequence(state, replyTo)(nextBehavior = inProgress)
    case msg: EditorAction           => inProgress(handleEditorAction(msg, state))
    case PullNext(replyTo)           => inProgress(state.pullNextStep(replyTo))
    case MaybeNext(replyTo)          => replyTo ! MaybeNextResult(state.stepList.flatMap(_.nextExecutable)); Behaviors.same
    case ReadyToExecuteNext(replyTo) => inProgress(state.readyToExecuteNext(replyTo))
    case Update(submitResponse, _)   => inProgress(state.updateStepStatus(submitResponse))
    case _: GoIdle                   => idle(state)
  }

  private def offline(state: SequencerActorState): Behavior[SequencerMsg] = receive(Offline) {
    case msg: CommonMessage => handleCommonMessage(msg, state)
    case GoOnline(replyTo)  => goOnline(replyTo, state)(fallbackBehavior = offline, nextBehavior = idle)
  }

  private def shuttingDown() = receive(ShuttingDown) {
    case ShutdownComplete(replyTo) =>
      replyTo ! Ok
      actorSystem.terminate()
      Behaviors.stopped
  }

  private def goingOnline(state: SequencerActorState)(
      fallbackBehavior: SequencerActorState => Behavior[SequencerMsg],
      nextBehavior: SequencerActorState => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] =
    receive(GoingOnline) {
      case msg: CommonMessage       => handleCommonMessage(msg, state)
      case GoOnlineSuccess(replyTo) => replyTo ! Ok; nextBehavior(state)
      case GoOnlineFailed(replyTo)  => replyTo ! GoOnlineHookFailed(); fallbackBehavior(state)
    }

  private def goingOffline(state: SequencerActorState): Behavior[SequencerMsg] = receive(GoingOffline) {
    case msg: CommonMessage   => handleCommonMessage(msg, state)
    case GoneOffline(replyTo) => replyTo ! Ok; offline(state.copy(stepList = None))
  }

  private def handleCommonMessage(message: CommonMessage, state: SequencerActorState): Behavior[SequencerMsg] = message match {
    case Shutdown(replyTo)            => shutdown(state, replyTo)
    case GetSequence(replyTo)         => sendStepListResponse(replyTo, state.stepList)
    case GetPreviousSequence(replyTo) => sendStepListResponse(replyTo, state.previousStepList)
  }

  private def handleEditorAction(editorAction: EditorAction, state: SequencerActorState): SequencerActorState = {
    import state._
    editorAction match {
      case Add(commands, replyTo)             => updateStepList(replyTo, stepList.map(_.append(commands)))
      case Pause(replyTo)                     => updateStepListResult(replyTo, stepList.map(_.pause))
      case Resume(replyTo)                    => updateStepList(replyTo, stepList.map(_.resume))
      case Replace(id, commands, replyTo)     => updateStepListResult(replyTo, stepList.map(_.replace(id, commands)))
      case Prepend(commands, replyTo)         => updateStepList(replyTo, stepList.map(_.prepend(commands)))
      case Delete(id, replyTo)                => updateStepListResult(replyTo, stepList.map(_.delete(id)))
      case InsertAfter(id, commands, replyTo) => updateStepListResult(replyTo, stepList.map(_.insertAfter(id, commands)))
      case AddBreakpoint(id, replyTo)         => updateStepListResult(replyTo, stepList.map(_.addBreakpoint(id)))
      case RemoveBreakpoint(id, replyTo)      => updateStepListResult(replyTo, stepList.map(_.removeBreakpoint(id)))
    }
  }

  private def abortSequence(state: SequencerActorState, replyTo: ActorRef[OkOrUnhandledResponse])(
      nextBehavior: SequencerActorState => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    script.executeAbort().onComplete(_ => state.self ! AbortSequenceComplete(replyTo))
    abortingSequence(state)(nextBehavior)
  }

  private def load(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse], state: SequencerActorState)(
      nextBehavior: SequencerActorState => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] =
    createStepList(sequence, state) match {
      case Left(err)       => replyTo ! err; Behaviors.same
      case Right(newState) => replyTo ! Ok; nextBehavior(newState)
    }

  private def start(state: SequencerActorState, replyTo: ActorRef[SequenceResponse]): Behavior[SequencerMsg] =
    inProgress(state.startSequence(replyTo))

  private def loadAndStart(
      sequence: Sequence,
      state: SequencerActorState,
      replyTo: ActorRef[SequenceResponse]
  ): Behavior[SequencerMsg] =
    createStepList(sequence, state) match {
      case Left(err)       => replyTo ! err; Behaviors.same
      case Right(newState) => start(newState, replyTo)
    }

  private def createStepList(
      sequence: Sequence,
      state: SequencerActorState
  ): Either[DuplicateIdsFound, SequencerActorState] =
    StepList(sequence).map(currentStepList => state.copy(stepList = Some(currentStepList), previousStepList = state.stepList))

  private def sendStepListResponse(replyTo: ActorRef[StepListResponse], stepList: Option[StepList]): Behavior[SequencerMsg] = {
    replyTo ! StepListResult(stepList)
    Behaviors.same
  }

  private def shutdown(state: SequencerActorState, replyTo: ActorRef[OkOrUnhandledResponse]): Behavior[SequencerMsg] = {

    // run both the futures in parallel and wait for both to complete
    // once all finished, send ShutdownComplete self message irrespective of any failures
    val f1 = locationService.unregister(AkkaConnection(componentId))
    val f2 = script.executeShutdown()
    f1.onComplete(_ => f2.onComplete(_ => state.self ! ShutdownComplete(replyTo)))

    shuttingDown()
  }

  private def abortingSequence(
      state: SequencerActorState
  )(nextBehavior: SequencerActorState => Behavior[SequencerMsg]): Behavior[SequencerMsg] =
    receive[AbortSequenceMessage](AbortingSequence) {
      case message: CommonMessage => handleCommonMessage(message, state)
      case AbortSequenceComplete(replyTo) =>
        import state._
        val maybeStepList = stepList.flatMap { x =>
          val inProgressStepList = x.discardPending
          if (inProgressStepList.steps.isEmpty) None
          else Some(inProgressStepList)
        }
        nextBehavior(updateStepList(replyTo, maybeStepList))
    }

  private def goOnline(replyTo: ActorRef[GoOnlineResponse], state: SequencerActorState)(
      fallbackBehavior: SequencerActorState => Behavior[SequencerMsg],
      nextBehavior: SequencerActorState => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    script.executeGoOnline().onComplete {
      case Success(_) => state.self ! GoOnlineSuccess(replyTo)
      case Failure(_) => state.self ! GoOnlineFailed(replyTo)
    }
    goingOnline(state)(fallbackBehavior, nextBehavior)
  }

  private def goOffline(replyTo: ActorRef[OkOrUnhandledResponse], state: SequencerActorState): Behavior[SequencerMsg] = {
    // go to offline state even if handler fails, note that this is different than GoOnline
    script.executeGoOffline().onComplete(_ => state.self ! GoneOffline(replyTo))
    goingOffline(state)
  }
}
