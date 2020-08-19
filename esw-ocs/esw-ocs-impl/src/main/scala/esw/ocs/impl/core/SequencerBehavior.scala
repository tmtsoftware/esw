package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.command.client.messages.{GetComponentLogMetadata, LogControlMessage, SetComponentLogLevel}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.logging.client.commons.LogAdminUtil
import csw.params.commands.Sequence
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.commons.Timeouts
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState
import esw.ocs.api.actor.messages.SequencerState._
import esw.ocs.api.codecs.OcsCodecs
import esw.ocs.api.protocol._
import esw.ocs.impl.script.ScriptApi

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

// The Sequencer Behavior's state transition is documented in Paradox Docs under topic State-transition
// Path to doc - $esw-paradox-site-link$/sequencer/state-transition.html
class SequencerBehavior(
    componentId: ComponentId,
    script: ScriptApi,
    locationService: LocationService,
    sequenceComponentPrefix: Prefix,
    logger: Logger,
    shutdownHttpService: () => Future[Done]
)(implicit val actorSystem: ActorSystem[_])
    extends OcsCodecs {
  import actorSystem.executionContext
  import logger._

  // Mapping of Sequencer state against corresponding state's behavior
  private def stateMachine(state: SequencerState[_]): SequencerData => Behavior[SequencerMsg] =
    state match {
      case Idle             => idle
      case Loaded           => loaded
      case InProgress       => inProgress
      case Offline          => offline
      case GoingOnline      => goingOnline(_, Offline)
      case GoingOffline     => goingOffline(_, Idle)
      case AbortingSequence => abortingSequence
      case Stopping         => stopping
      case Submitting       => submitting
      case Starting         => startingSequence
    }

  // Starting point of the Sequencer
  def setup: Behavior[SequencerMsg]              = Behaviors.setup { ctx => idle(SequencerData.initial(ctx.self)) }
  private lazy val SequenceFailedToStartMessage  = "New sequence handler failed to start successfully"
  private lazy val SequenceFailedToSubmitMessage = "New sequence handler failed to submit successfully"
  // ******************* Sequencer Behaviors **************

  private def idle(data: SequencerData): Behavior[SequencerMsg] =
    receive(Idle, data) {
      case GoOffline(replyTo)                        => goOffline(replyTo, data, Idle)
      case GoOnline(replyTo)                         => goOnline(replyTo, data, Idle)
      case LoadSequence(sequence, replyTo)           => load(sequence, replyTo, data)
      case SubmitSequenceInternal(sequence, replyTo) => submitSequence(sequence, data, replyTo)
      case PullNext(replyTo)                         => idle(data.pullNextStep(replyTo)) // registers a subscriber for Step
    }

  private def loaded(data: SequencerData): Behavior[SequencerMsg] =
    receive(Loaded, data) {
      case GoOffline(replyTo)              => goOffline(replyTo, data, Loaded)
      case GoOnline(replyTo)               => goOnline(replyTo, data, Loaded)
      case msg: EditorAction               => handleEditorAction(msg, data, currentState = Loaded)
      case StartSequence(replyTo)          => startSequence(data, replyTo)
      case LoadSequence(sequence, replyTo) => load(sequence, replyTo, data)
    }

  private def inProgress(data: SequencerData): Behavior[SequencerMsg] =
    receive(InProgress, data) {
      case AbortSequence(replyTo) => abortSequence(data, replyTo)
      case Stop(replyTo)          => stop(data, replyTo)
      case msg: EditorAction      => handleEditorAction(msg, data, currentState = InProgress)
      case Pause(replyTo)         => inProgress(data.updateStepListResult(replyTo, data.stepList.map(_.pause)))
      case Resume(replyTo)        => inProgress(data.updateStepList(replyTo, data.stepList.map(_.resume)))
      case PullNext(replyTo)      => inProgress(data.pullNextStep(replyTo))
      case StepSuccess(_)         => inProgress(data.stepSuccess(InProgress))
      case StepFailure(reason, _) => inProgress(data.stepFailure(reason, InProgress))
      case _: GoIdle              => idle(data) // this is received on sequence completion
    }

  private def offline(data: SequencerData): Behavior[SequencerMsg] =
    receive(Offline, data) {
      case GoOnline(replyTo)  => goOnline(replyTo, data, Offline)
      case GoOffline(replyTo) => goOffline(replyTo, data, Offline)
    }

  // Starts executing GoOnline handlers of script and changes state to intermediate state GoingOnline
  // On successful execution of handlers, state will change to Idle.
  private def goOnline(
      replyTo: ActorRef[GoOnlineResponse],
      data: SequencerData,
      currentState: SequencerState[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    script.executeGoOnline().onComplete {
      case Success(_) =>
        debug("Successfully executed GoOnline script handlers")
        data.self ! GoOnlineSuccess(replyTo)
      case Failure(e) =>
        error(s"Failed while executing GoOnline script handlers with error : ${e.getMessage}")
        data.self ! GoOnlineFailed(replyTo)
    }
    goingOnline(data, currentState)
  }

  private def goingOnline(data: SequencerData, currentState: SequencerState[SequencerMsg]): Behavior[SequencerMsg] = {
    val currentBehavior = stateMachine(currentState)
    receive(GoingOnline, data) {
      case GoOnlineSuccess(replyTo) =>
        replyTo ! Ok
        if (currentState == Offline) idle(data) else currentBehavior(data)
      case GoOnlineFailed(replyTo) => replyTo ! GoOnlineHookFailed; currentBehavior(data)
    }
  }

  // This can only be received in Idle or InProgress state.
  // Starts executing the goOffline handlers and changes state to intermediate state GoingOffline
  // On successful execution of handlers, state will change to Offline, otherwise to the previous state
  private def goOffline(
      replyTo: ActorRef[GoOfflineResponse],
      data: SequencerData,
      currentState: SequencerState[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    script.executeGoOffline().onComplete {
      case Success(_) =>
        debug("Successfully executed GoOffline script handlers")
        data.self ! GoOfflineSuccess(replyTo)
      case Failure(e) =>
        error(s"Failed while executing GoOffline script handlers with error : ${e.getMessage}")
        data.self ! GoOfflineFailed(replyTo)
    }
    goingOffline(data, currentState)
  }

  private def goingOffline(data: SequencerData, currentState: SequencerState[SequencerMsg]): Behavior[SequencerMsg] =
    receive(GoingOffline, data) {
      case GoOfflineSuccess(replyTo) => replyTo ! Ok; offline(data.copy(stepList = None))
      case GoOfflineFailed(replyTo) =>
        val currentBehavior = stateMachine(currentState)
        replyTo ! GoOfflineHookFailed; currentBehavior(data)
    }

  // Only called from InProgress state. Method starts executing abort handlers and changes state to
  // intermediate state AbortingSequence. On completion of handlers, pending steps will be discarded and
  // state will change to InProgress.
  private def abortSequence(data: SequencerData, replyTo: ActorRef[OkOrUnhandledResponse]): Behavior[SequencerMsg] = {
    script.executeAbort().onComplete(_ => data.self ! AbortSequenceComplete(replyTo))
    abortingSequence(data)
  }

  private def abortingSequence(data: SequencerData): Behavior[SequencerMsg] =
    receive(AbortingSequence, data) {
      case AbortSequenceComplete(replyTo) =>
        import data._
        val maybeStepList = stepList.map(_.discardPending)
        inProgress(updateStepList(replyTo, maybeStepList))
    }

  // Only called from InProgress state. Method starts executing stop handlers and changes state to
  // intermediate state Stopping. On completion of handlers, pending steps will be discarded and
  // state will change to InProgress
  private def stop(data: SequencerData, replyTo: ActorRef[OkOrUnhandledResponse]): Behavior[SequencerMsg] = {
    script.executeStop().onComplete(_ => data.self ! StopComplete(replyTo))
    stopping(data)
  }

  private def stopping(data: SequencerData): Behavior[SequencerMsg] =
    receive[StopMessage](Stopping, data) {
      case StopComplete(replyTo) =>
        import data._
        val maybeStepList = stepList.map(_.discardPending)
        inProgress(updateStepList(replyTo, maybeStepList))
    }

  // This is a common message and can be received in any state. This will do the following things
  // 1. unregister from location service.
  // 2. execute shutdown handlers of script.
  // 3. shutdowns the Sequencers HTTP server.
  // 4. Changes state to intermediate state ShuttingDown.
  // On completion of the first 3 tasks, the actor system will be terminated and Sequencer actor will be stopped.
  private def shutdown(data: SequencerData, replyTo: ActorRef[Ok.type]): Behavior[SequencerMsg] = {

    // run the futures in parallel and wait for all of them to complete
    // once all finished, send ShutdownComplete self message irrespective of any failures
    val f1 = locationService.unregister(AkkaConnection(componentId))
    val f2 = script.executeShutdown() // execute shutdown handlers of script
    val f3 = shutdownHttpService()
    f1.onComplete(_ =>
      f2.onComplete { _ =>
        script.shutdownScript() // to clean up the script. For eg. to stop StrandEc.
        f3.onComplete(_ => replyTo ! Ok)
      }
    )

    Behaviors.stopped
  }

  // This is a common message and can be received in any state.
  // This will execute the Diagnostic mode handlers of the script.
  private def goToDiagnosticMode(
      startTime: UTCTime,
      hint: String,
      replyTo: ActorRef[DiagnosticModeResponse]
  ): Behavior[SequencerMsg] = {
    script.executeDiagnosticMode(startTime, hint).onComplete {
      case Success(_) =>
        debug("Successfully executed diagnostic mode script handlers")
        replyTo ! Ok
      case Failure(e) =>
        error(s"Failed while executing diagnostic mode script handlers with error : ${e.getMessage}")
        replyTo ! DiagnosticHookFailed
    }
    Behaviors.same
  }

  // This is a common message and can be received in any state.
  // This will execute the Operations mode handlers of the script.
  private def goToOperationsMode(replyTo: ActorRef[OperationsModeResponse]): Behavior[SequencerMsg] = {
    script.executeOperationsMode().onComplete {
      case Success(_) =>
        debug("Successfully executed operations mode script handlers")
        replyTo ! Ok
      case Failure(e) =>
        error(s"Failed while executing operations mode script handlers with error : ${e.getMessage}")
        replyTo ! OperationsHookFailed
    }
    Behaviors.same
  }

  private def load(sequence: Sequence, replyTo: ActorRef[OkOrUnhandledResponse], data: SequencerData): Behavior[SequencerMsg] = {
    replyTo ! Ok
    loaded(data.createStepList(sequence))
  }

  // Loads and starts the sequence execution. Changes Sequencer state to InProgress.
  private def submitSequence(
      sequence: Sequence,
      data: SequencerData,
      replyTo: ActorRef[SequencerSubmitResponse]
  ): Behavior[SequencerMsg] = {
    script.executeNewSequenceHandler().onComplete {
      case Success(_) => data.self ! SubmitSuccessful(sequence, replyTo)
      case Failure(_) => data.self ! SubmitFailed(replyTo)
    }
    submitting(data)
  }

  private def submitting(data: SequencerData): Behavior[SequencerMsg] =
    receive[SubmitMessage](Submitting, data) {
      case SubmitSuccessful(sequence, replyTo) => inProgress(data.createStepList(sequence).startSequence(replyTo))
      case SubmitFailed(replyTo)               => replyTo ! NewSequenceHookFailed(SequenceFailedToSubmitMessage); idle(data)
    }

  private def startSequence(
      data: SequencerData,
      replyTo: ActorRef[SequencerSubmitResponse]
  ): Behavior[SequencerMsg] = {
    script.executeNewSequenceHandler().onComplete {
      case Success(_) => data.self ! StartingSuccessful(replyTo)
      case Failure(_) => data.self ! StartingFailed(replyTo)
    }
    startingSequence(data)
  }

  private def startingSequence(data: SequencerData): Behavior[SequencerMsg] =
    receive[StartingMessage](Starting, data) {
      case StartingSuccessful(replyTo) => inProgress(data.startSequence(replyTo))
      case StartingFailed(replyTo) =>
        replyTo ! NewSequenceHookFailed(SequenceFailedToStartMessage); loaded(data)
    }

  private def handleCommonMessage[T <: SequencerMsg](
      message: CommonMessage,
      state: SequencerState[T],
      data: SequencerData
  ): Behavior[SequencerMsg] =
    message match {
      case Shutdown(replyTo)                        => shutdown(data, replyTo)
      case GetSequence(replyTo)                     => replyTo ! data.stepList; Behaviors.same
      case GetSequencerState(replyTo)               => replyTo ! state; Behaviors.same
      case DiagnosticMode(startTime, hint, replyTo) => goToDiagnosticMode(startTime, hint, replyTo)
      case OperationsMode(replyTo)                  => goToOperationsMode(replyTo)
      case GetSequenceComponent(replyTo) => {
        locationService
          .find(AkkaConnection(ComponentId(sequenceComponentPrefix, SequenceComponent)))
          .map(replyTo ! _.get)
        Behaviors.same
      }
      case ReadyToExecuteNext(replyTo) => stateMachine(state)(data.readyToExecuteNext(replyTo))
      case MaybeNext(replyTo) =>
        if (state == InProgress) replyTo ! data.stepList.flatMap(_.nextExecutable)
        else replyTo ! None
        Behaviors.same
    }

  // handles all the sequence editor messages. These messages are supported only in Loaded and InProgress state,
  // State remains the same only except the Reset message in Loaded state, where it changes to Idle.
  private def handleEditorAction(
      editorAction: EditorAction,
      data: SequencerData,
      currentState: SequencerState[SequencerMsg]
  ): Behavior[SequencerMsg] = {
    import data._
    val currentBehavior = stateMachine(currentState)
    editorAction match {
      case Add(commands, replyTo)                   => currentBehavior(updateStepList(replyTo, stepList.map(_.append(commands))))
      case Prepend(commands, replyTo)               => currentBehavior(updateStepList(replyTo, stepList.map(_.prepend(commands))))
      case Delete(id, replyTo)                      => currentBehavior(updateStepListResult(replyTo, stepList.map(_.delete(id))))
      case Reset(replyTo) if currentState == Loaded => idle(updateStepList(replyTo, stepList = None))
      case Reset(replyTo)                           => currentBehavior(updateStepList(replyTo, stepList.map(_.discardPending)))
      case Replace(id, commands, replyTo) =>
        currentBehavior(updateStepListResult(replyTo, stepList.map(_.replace(id, commands))))
      case InsertAfter(id, commands, replyTo) =>
        currentBehavior(updateStepListResult(replyTo, stepList.map(_.insertAfter(id, commands))))
      case AddBreakpoint(id, replyTo) => currentBehavior(updateStepListResult(replyTo, stepList.map(_.addBreakpoint(id))))
      case RemoveBreakpoint(id, replyTo) =>
        currentBehavior(updateStepListResult(replyTo, stepList.map(_.removeBreakpoint(id))))
    }
  }

  private def handleLogMessages(msg: LogControlMessage): Unit =
    msg match {
      case GetComponentLogMetadata(replyTo) => replyTo ! LogAdminUtil.getLogMetadata(componentId.prefix)
      case SetComponentLogLevel(logLevel)   => LogAdminUtil.setComponentLogLevel(componentId.prefix, logLevel)
    }

  // Helper Function which returns a Behavior for a particular Sequencer state when given message handler for that particular state.
  // Returns a State handler which is combination of handlers of - Common message + CSW messages + State specific messages
  // (handler taken as parameter). All other messages will be treated as Unhandled.
  private def receive[StateMessage <: SequencerMsg: ClassTag](
      state: SequencerState[StateMessage],
      data: SequencerData
  )(stateHandler: StateMessage => Behavior[SequencerMsg]): Behavior[SequencerMsg] =
    Behaviors.receive { (ctx, msg) =>
      implicit val timeout: Timeout = Timeouts.LongTimeout
      debug(s"Sequencer in State: $state, received Message: $msg")

      msg match {
        // ********* ESW Sequencer Messages *******
        case msg: CommonMessage => handleCommonMessage(msg, state, data)
        case msg: StateMessage  => stateHandler(msg)
        case msg: UnhandleableSequencerMessage =>
          msg.replyTo ! Unhandled(state.entryName, msg.getClass.getSimpleName); Behaviors.same

        // ********* CSW Sequencer Messages *******
        // SubmitSequence is a CSW SequencerMsg, to be able to handle it only in Idle State, a corresponding Internal message is created
        // and handled in Idle State. If this message is received in any other state, and Unhandled response is returned which is
        // then adapted to a SubmitResponse
        case SubmitSequence(sequence, replyTo) =>
          val submitResponse: Future[SequencerSubmitResponse] = ctx.self ? (SubmitSequenceInternal(sequence, _))
          submitResponse.foreach(res => replyTo ! res.toSubmitResponse())
          Behaviors.same

        case msg: LogControlMessage => handleLogMessages(msg); Behaviors.same

        case Query(runId, replyTo) => data.query(runId, replyTo); Behaviors.same
        // Behaviors.same is not used below, because new SequencerData (updated with subscribers) needs to be passed to currentBehavior
        case QueryFinal(runId, replyTo) => stateMachine(state)(data.queryFinal(runId, replyTo))

        case _ => Behaviors.unhandled
      }
    }
}
