package esw.ocs.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.commands.{CommandResponse, Sequence}
import csw.params.core.models.Id
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages._
import esw.ocs.api.models.{SequencerState, Step, StepList, StepStatus}
import esw.ocs.dsl.ScriptDsl

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class SequencerBehavior(
    componentId: ComponentId,
    script: ScriptDsl,
    locationService: LocationService,
    crm: CommandResponseManager
)(implicit val actorSystem: ActorSystem[_], timeout: Timeout)
    extends OcsFrameworkCodecs {

  private val emptyChildId = Id("empty-child") // fixme

  //BEHAVIOURS
  def idle(ss: SequencerState): Behavior[EswSequencerMessage] = receive[IdleMessage]("idle") { (ctx, msg) =>
    val state = ss.copy(self = Some(ctx.self))
    import ctx._
    msg match {
      case x: AnyStateMessage => handleAnyStateMessage(x, state, _ => ctx.system.terminate)
      // ===== External Lifecycle =====
      case GoOffline(Some(replyTo)) => goOffline(replyTo, state)
      // ===== External Editor =====
      case LoadSequence(sequence, Some(replyTo))   => load(sequence, replyTo, state)
      case LoadAndProcess(sequence, Some(replyTo)) => loadAndProcess(sequence, state, replyTo)

      // ===== Internal =====
      case PullNext(Some(replyTo)) => pullNext(state, replyTo, idle)
    }
  }

  def loaded(ss: SequencerState): Behavior[EswSequencerMessage] = receive[SequenceLoadedMessage]("loaded") { (ctx, msg) =>
    import ctx._
    val state = ss.copy(self = Some(ctx.self))
    msg match {
      case x: AnyStateMessage           => handleAnyStateMessage(x, state, _ => ctx.system.terminate)
      case x: EditorAction              => loaded(handleEditorAction(x, state))
      case GoOffline(Some(replyTo))     => goOffline(replyTo, state)
      case StartSequence(Some(replyTo)) => process(state, replyTo)
    }
  }

  def inProgress(ss: SequencerState): Behavior[EswSequencerMessage] = receive[InProgressMessage]("in-progress") { (ctx, msg) =>
    import ctx._
    val state = ss.copy(self = Some(ctx.self))
    msg match {
      case x: AnyStateMessage                => handleAnyStateMessage(x, state, _ => ctx.system.terminate)
      case editorAction: EditorAction        => inProgress(handleEditorAction(editorAction, state))
      case PullNext(Some(replyTo))           => pullNext(state, replyTo, inProgress)
      case MaybeNext(Some(replyTo))          => replyTo ! MaybeNextResult(state.stepList.nextExecutable); Behaviors.same
      case ReadyToExecuteNext(Some(replyTo)) => readyToExecuteNext(state, replyTo, inProgress)
      case UpdateFailure(failureResponse, _) => inProgress(updateFailure(failureResponse, state))
      case UpdateSequencerState(newState, _) => inProgress(newState)
    }
  }

  def handleAnyStateMessage(message: AnyStateMessage, state: SequencerState, killFunction: Unit => Unit)(
      implicit ec: ExecutionContext
  ): Behavior[EswSequencerMessage] = message match {
    case Shutdown(Some(replyTo))            => shutdown(replyTo, killFunction)
    case GetPreviousSequence(Some(replyTo)) => getPreviousSequence(replyTo, state)
  }

  def handleEditorAction(editorAction: EditorAction, state: SequencerState)(implicit ec: ExecutionContext): SequencerState = {
    editorAction match {
      case Abort(Some(replyTo))                     => ??? //story not played yet
      case GetSequence(Some(replyTo))               => getSequence(replyTo, state)
      case Add(commands, Some(replyTo))             => updateStepList1(replyTo, state, state.stepList.append(commands))
      case Pause(Some(replyTo))                     => updateStepList(replyTo, state, state.stepList.pause)
      case Resume(Some(replyTo))                    => updateStepList1(replyTo, state, state.stepList.resume)
      case Reset(Some(replyTo))                     => updateStepList1(replyTo, state, state.stepList.discardPending)
      case Replace(id, commands, Some(replyTo))     => updateStepList(replyTo, state, state.stepList.replace(id, commands))
      case Prepend(commands, Some(replyTo))         => updateStepList1(replyTo, state, state.stepList.prepend(commands))
      case Delete(id, Some(replyTo))                => updateStepList(replyTo, state, state.stepList.delete(id))
      case InsertAfter(id, commands, Some(replyTo)) => updateStepList(replyTo, state, state.stepList.insertAfter(id, commands))
      case AddBreakpoint(id, Some(replyTo))         => updateStepList(replyTo, state, state.stepList.addBreakpoint(id))
      case RemoveBreakpoint(id, Some(replyTo))      => updateStepList(replyTo, state, state.stepList.removeBreakpoint(id))
    }
  }

  def offline(ss: SequencerState): Behavior[EswSequencerMessage] = receive[OfflineMessage]("offline") { (ctx, message) =>
    import ctx._
    val state = ss.copy(self = Some(ctx.self))
    message match {
      case GoOnline(Some(replyTo))            => goOnline(replyTo, state)
      case Shutdown(Some(replyTo))            => shutdown(replyTo, _ => ctx.system.terminate)
      case GetPreviousSequence(Some(replyTo)) => getPreviousSequence(replyTo, state)
    }
  }

  // $COVERAGE-OFF$

  def pullNext(
      state: SequencerState,
      replyTo: ActorRef[PullNextResult],
      behaviour: SequencerState => Behavior[EswSequencerMessage]
  )(
      implicit executionContext: ExecutionContext
  ): Behavior[EswSequencerMessage] = {
    val newState = state.copy(stepRefSubscriber = Some(replyTo))
    behaviour(tryExecutingNextPendingStep(newState))
  }

  def process0(state: SequencerState)(
      implicit executionContext: ExecutionContext
  ): (SequencerState, Future[SubmitResponse]) = {
    val id = state.stepList.runId
    crm.addOrUpdateCommand(Started(id))
    crm.addSubCommand(id, emptyChildId)
    val newState = state.stepList.nextExecutable
      .collect {
        case step if step.isPending =>
          val (newStep, newState) = setPendingToInFlight(step, state)
          sendStepToSubscriber(newState, newStep)
      }
      .getOrElse(state)
    newState.readyToExecuteSubscriber.foreach(_ ! Ok)
    //fixme: this does not handle future failure
    //fixme: this should send message immediately and not after sequence is finished
    (newState, handleSequenceResponse(crm.queryFinal(id), newState))
  }

  def process(state: SequencerState, replyTo: ActorRef[Ok.type])(
      implicit executionContext: ExecutionContext
  ): Behavior[EswSequencerMessage] = {
    val (newState, submitResponseF) = process0(state)
    submitResponseF.foreach(_ => replyTo ! Ok)
    inProgress(newState)
  }

  def sendStepToSubscriber(state: SequencerState, step: Step): SequencerState = {
    state.stepRefSubscriber.foreach(_ ! PullNextResult(step))
    state.copy(stepRefSubscriber = None)
  }

  def load1(
      sequence: Sequence,
      state: SequencerState
  ): Either[DuplicateIdsFound.type, SequencerState] =
    StepList(sequence).map(x => state.copy(stepList = x, previousStepList = Some(state.stepList)))

  def load(
      sequence: Sequence,
      replyTo: ActorRef[LoadSequenceResponse],
      state: SequencerState
  ): Behavior[EswSequencerMessage] = load1(sequence, state) match {
    case Left(err) =>
      replyTo ! err
      Behaviors.same
    case Right(newState) =>
      replyTo ! Ok
      loaded(newState)
  }

  protected def receive[B <: EswSequencerMessage: ClassTag](stateName: String)(
      f: (ActorContext[EswSequencerMessage], B) => Behavior[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case m: B => f(ctx, m)
        case _    =>
          //fixme: handle the Unhandled
          //m.replyTo ! Unhandled(stateName, m.getClass.getSimpleName)
          Behaviors.same
      }
    }

  private def readyToExecuteNext(
      state: SequencerState,
      replyTo: ActorRef[ReadyToExecuteNextResponse],
      behaviour: SequencerState => Behavior[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] = {
    val newState = if (state.stepList.isInFlight || state.stepList.isFinished) {
      state.copy(readyToExecuteSubscriber = Some(replyTo))
    } else {
      replyTo ! Ok
      state.copy(readyToExecuteSubscriber = None)
    }
    behaviour(newState)
  }

  private def getSequence(replyTo: ActorRef[GetSequenceResult], state: SequencerState): SequencerState = {
    replyTo ! GetSequenceResult(state.stepList)
    state
  }

  private def getPreviousSequence(
      replyTo: ActorRef[GetPreviousSequenceResult],
      state: SequencerState
  ): Behavior[EswSequencerMessage] = {
    replyTo ! GetPreviousSequenceResult(state.previousStepList)
    Behaviors.same
  }

  private def tryExecutingNextPendingStep(state: SequencerState)(
      implicit executionContext: ExecutionContext
  ): SequencerState =
    state.stepList.nextExecutable
      .map { pendingStep =>
        val (step, newState) = setPendingToInFlight(pendingStep, state)
        state.stepRefSubscriber.foreach(_ ! PullNextResult(step))
        newState
      }
      .getOrElse(state)

  private def loadAndProcess(
      sequence: Sequence,
      state: SequencerState,
      replyTo: ActorRef[LoadAndProcessResponse]
  )(
      implicit executionContext: ExecutionContext
  ): Behavior[EswSequencerMessage] = {
    val newStateMayBe = load1(sequence, state)
    newStateMayBe match {
      case Left(err) =>
        replyTo ! err
        Behaviors.same
      case Right(newState) =>
        val (s, f) = process0(newState)
        f.foreach(_ => replyTo ! Ok) // todo: log errors
        inProgress(s)
    }
  }

  private def goToIdle(state: SequencerState): Unit = {
    state.self.foreach(_ ! GoIdle(state, None))
  }

  private def handleSequenceResponse(
      submitResponse: Future[SubmitResponse],
      state: SequencerState
  )(implicit ec: ExecutionContext): Future[SubmitResponse] = {
    submitResponse.onComplete(_ => goToIdle(state))
    submitResponse.map(CommandResponse.withRunId(state.stepList.runId, _))
  }

  // this method gets called from places where it is already checked that step is in pending status
  private def setPendingToInFlight(
      step: Step,
      state: SequencerState
  )(implicit ec: ExecutionContext): (Step, SequencerState) = {
    val inflightStep = step.setPendingToInFlight()
    val newState     = state.copy(stepList = state.stepList.updateStep(inflightStep))
    val stepRunId    = step.id
    crm.addSubCommand(newState.stepList.runId, stepRunId)
    crm.addOrUpdateCommand(CommandResponse.Started(stepRunId))
    processStepResponse(stepRunId, crm.queryFinal(stepRunId), newState)
    (inflightStep, newState)
  }

  private def processStepResponse(
      stepId: Id,
      submitResponseF: Future[SubmitResponse],
      state: SequencerState
  )(
      implicit executionContext: ExecutionContext
  ): Future[SequencerState] =
    submitResponseF
      .map {
        case submitResponse if CommandResponse.isPositive(submitResponse) => updateSuccess(submitResponse, state)
        case failureResponse                                              => updateFailure(failureResponse, state)
      }
      .recoverWith {
        case NonFatal(e) => Future.successful(updateFailure(Error(stepId, e.getMessage), state))
      }

  private def updateSuccess(
      successResponse: SubmitResponse,
      state: SequencerState
  )(implicit ec: ExecutionContext): SequencerState =
    updateStepStatus(successResponse, Finished.Success(successResponse), state)

  private[ocs] def updateFailure(
      failureResponse: SubmitResponse,
      state: SequencerState
  )(implicit ec: ExecutionContext): SequencerState =
    updateStepStatus(failureResponse, Finished.Failure(failureResponse), state)

  private def updateStepStatus(
      submitResponse: SubmitResponse,
      stepStatus: StepStatus,
      state: SequencerState
  )(
      implicit ec: ExecutionContext
  ): SequencerState = {
    crm.updateSubCommand(submitResponse)
    val newStepList = state.stepList.updateStatus(submitResponse.runId, stepStatus)
    state.self.foreach(_ ! UpdateSequencerState(state.copy(stepList = newStepList), null))
    checkForSequenceCompletion(state)

    if (!state.stepList.isFinished) state.readyToExecuteSubscriber.foreach(_ ! Ok)

    state.copy(stepList = newStepList)
  }

  private def checkForSequenceCompletion(state: SequencerState): Unit = if (state.stepList.isFinished) {
    crm.updateSubCommand(Completed(emptyChildId))
  }

  private def shutdown(replyTo: ActorRef[Ok.type], killFunction: Unit => Unit)(
      implicit ec: ExecutionContext
  ): Behavior[EswSequencerMessage] = {

    locationService.unregister(AkkaConnection(componentId))
    script.executeShutdown().onComplete { _ =>
      replyTo ! Ok
      killFunction
    }
    Behaviors.stopped[EswSequencerMessage]
  }

  private def updateStepList[T <: EditorError](
      replyTo: ActorRef[Ok.type],
      state: SequencerState,
      stepListResultFunc: => Either[T, StepList]
  )(
      implicit executionContext: ExecutionContext
  ): SequencerState = {
    val stepListResult: Either[EditorError, StepList] = stepListResultFunc
    val newStepList                                   = stepListResult.getOrElse(state.stepList) // handle failure
    val newState                                      = state.copy(stepList = newStepList)
    replyTo ! Ok
    checkForSequenceCompletion(newState)
    tryExecutingNextPendingStep(newState)
  }

  private def updateStepList1[T <: EditorError](
      replyTo: ActorRef[Ok.type],
      state: SequencerState,
      stepList: => StepList
  )(
      implicit executionContext: ExecutionContext
  ): SequencerState = {
    val newState = state.copy(stepList)
    replyTo ! Ok
    checkForSequenceCompletion(newState)
    tryExecutingNextPendingStep(newState)
  }

  //HANDLERS
  private def goOnline(replyTo: ActorRef[Ok.type], state: SequencerState): Behavior[EswSequencerMessage] = {
    script.executeGoOnline() // recover and log
    replyTo ! Ok
    idle(state)
  }
  private def goOffline(replyTo: ActorRef[Ok.type], state: SequencerState): Behavior[EswSequencerMessage] = {
    replyTo ! Ok
    script.executeGoOffline() // recover and log
    offline(SequencerState.initial.copy(state.stepList))
  }

  // $COVERAGE-ON$

}
