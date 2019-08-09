package esw.ocs.core

import akka.Done
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

import scala.concurrent.duration.DurationInt
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

  private val atMost = 5.seconds

  //BEHAVIOURS
  def idle(state: SequencerState): Behavior[EswSequencerMessage] = receive[IdleMessage]("idle") { (ctx, msg) =>
    implicit val context: ActorContext[EswSequencerMessage] = ctx
    import ctx._
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo)  => shutdown(replyTo)
      case GoOffline(replyTo) => goOffline(replyTo, state)

      // ===== External Editor =====
      case LoadSequence(sequence, replyTo)   => load(sequence, Some(replyTo), state)
      case LoadAndProcess(sequence, replyTo) => loadAndProcess(sequence, state, replyTo)
      case GetPreviousSequence(replyTo)      => getPreviousSequence(replyTo, state)
      // ===== Internal =====
      case PullNext(replyTo) => pullNext(state, replyTo, idle)
    }
  }

  def loaded(state: SequencerState): Behavior[EswSequencerMessage] = receive[SequenceLoadedMessage]("loaded") { (ctx, msg) =>
    import ctx._
    implicit val context: ActorContext[EswSequencerMessage] = ctx
    msg match {
      case Shutdown(replyTo)      => shutdown(replyTo)
      case GoOffline(replyTo)     => goOffline(replyTo, state)
      case StartSequence(replyTo) => process(state, replyTo)

      case Abort(replyTo)                     => ??? //story not played yet
      case GetSequence(replyTo)               => getSequence(replyTo, state)
      case GetPreviousSequence(replyTo)       => getPreviousSequence(replyTo, state)
      case Add(commands, replyTo)             => loaded(updateStepList(replyTo, state, state.stepList.append(commands)))
      case Pause(replyTo)                     => loaded(updateStepList(replyTo, state, state.stepList.pause))
      case Resume(replyTo)                    => loaded(updateStepList(replyTo, state, state.stepList.resume))
      case Reset(replyTo)                     => loaded(updateStepList(replyTo, state, state.stepList.discardPending))
      case Replace(id, commands, replyTo)     => loaded(updateStepList(replyTo, state, state.stepList.replace(id, commands)))
      case Prepend(commands, replyTo)         => loaded(updateStepList(replyTo, state, state.stepList.prepend(commands)))
      case Delete(id, replyTo)                => loaded(updateStepList(replyTo, state, state.stepList.delete(id)))
      case InsertAfter(id, commands, replyTo) => loaded(updateStepList(replyTo, state, state.stepList.insertAfter(id, commands)))
      case AddBreakpoint(id, replyTo)         => loaded(updateStepList(replyTo, state, state.stepList.addBreakpoint(id)))
      case RemoveBreakpoint(id, replyTo)      => loaded(updateStepList(replyTo, state, state.stepList.removeBreakpoint(id)))
    }
  }

  def inProgress(state: SequencerState): Behavior[EswSequencerMessage] = receive[InProgressMessage]("in-progress") { (ctx, msg) =>
    implicit val context: ActorContext[EswSequencerMessage] = ctx
    import context._
    msg match {
      case Shutdown(replyTo)              => shutdown(replyTo)
      case Abort(replyTo)                 => ??? // story not played
      case GetSequence(replyTo)           => getSequence(replyTo, state)
      case GetPreviousSequence(replyTo)   => getPreviousSequence(replyTo, state)
      case Add(commands, replyTo)         => inProgress(updateStepList(replyTo, state, state.stepList.append(commands)))
      case Pause(replyTo)                 => inProgress(updateStepList(replyTo, state, state.stepList.pause))
      case Resume(replyTo)                => inProgress(updateStepList(replyTo, state, state.stepList.resume))
      case Reset(replyTo)                 => inProgress(updateStepList(replyTo, state, state.stepList.discardPending))
      case Replace(id, commands, replyTo) => inProgress(updateStepList(replyTo, state, state.stepList.replace(id, commands)))
      case Prepend(commands, replyTo)     => inProgress(updateStepList(replyTo, state, state.stepList.prepend(commands)))
      case Delete(id, replyTo)            => inProgress(updateStepList(replyTo, state, state.stepList.delete(id)))
      case InsertAfter(id, cmds, replyTo) => inProgress(updateStepList(replyTo, state, state.stepList.insertAfter(id, cmds)))
      case AddBreakpoint(id, replyTo)     => inProgress(updateStepList(replyTo, state, state.stepList.addBreakpoint(id)))
      case RemoveBreakpoint(id, replyTo)  => inProgress(updateStepList(replyTo, state, state.stepList.removeBreakpoint(id)))

      case PullNext(replyTo)              => pullNext(state, replyTo, inProgress)
      case MaybeNext(replyTo)             => replyTo ! state.stepList.nextExecutable; Behaviors.same
      case ReadyToExecuteNext(replyTo)    => readyToExecuteNext(state, replyTo, inProgress)
      case UpdateFailure(failureResponse) => inProgress(updateFailure(failureResponse, state))
      case UpdateSequencerState(newState) => inProgress(newState)
    }
  }

  def offline(state: SequencerState): Behavior[EswSequencerMessage] = receive[OfflineMessage]("offline") { (ctx, message) =>
    implicit val context: ActorContext[EswSequencerMessage] = ctx
    message match {
      case GoOnline(replyTo)            => goOnline(replyTo, state)
      case Shutdown(replyTo)            => shutdown(replyTo)
      case GetPreviousSequence(replyTo) => getPreviousSequence(replyTo, state)
    }
  }

  // $COVERAGE-OFF$

  def pullNext(state: SequencerState, replyTo: ActorRef[Step], behaviour: SequencerState => Behavior[EswSequencerMessage])(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] = {
    val newState = state.copy(stepRefSubscriber = Some(replyTo))
    behaviour(tryExecutingNextPendingStep(newState))
  }

  def process(state: SequencerState, replyTo: ActorRef[SubmitResponse])(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] = {
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
    newState.readyToExecuteSubscriber.foreach(_ ! Done)
    //fixme: this does not handle future failure
    //fixme: this should send message immediately and not after sequence is finished
    handleSequenceResponse(crm.queryFinal(id), newState).foreach(replyTo.tell)
    inProgress(newState)
  }

  def sendStepToSubscriber(state: SequencerState, step: Step): SequencerState = {
    state.stepRefSubscriber.foreach(_ ! step)
    state.copy(stepRefSubscriber = None)
  }

  def load(
      sequence: Sequence,
      replyTo: Option[ActorRef[LoadSequenceResponse]],
      state: SequencerState
  ): Behavior[EswSequencerMessage] = {
    StepList(sequence) match {
      case Left(duplicateIdsFound) =>
        replyTo.foreach(_ ! duplicateIdsFound)
        Behaviors.same
      case Right(x) =>
        replyTo.foreach(_ ! Ok)
        loaded(state.copy(stepList = x, previousStepList = Some(state.stepList)))
    }
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
      replyTo: ActorRef[Done],
      behaviour: SequencerState => Behavior[EswSequencerMessage]
  ) = {
    val newState = if (state.stepList.isInFlight || state.stepList.isFinished) {
      state.copy(readyToExecuteSubscriber = Some(replyTo))
    } else {
      replyTo ! Done
      state.copy(readyToExecuteSubscriber = None)
    }
    behaviour(newState)
  }

  private def getSequence(replyTo: ActorRef[StepList], state: SequencerState): Behavior[EswSequencerMessage] = {
    replyTo ! state.stepList
    Behaviors.same
  }

  private def getPreviousSequence(
      replyTo: ActorRef[GetPreviousSequenceResult],
      state: SequencerState
  ): Behavior[EswSequencerMessage] = {
    replyTo ! GetPreviousSequenceResult(state.previousStepList)
    Behaviors.same
  }

  private def tryExecutingNextPendingStep(state: SequencerState)(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): SequencerState =
    state.stepList.nextExecutable
      .map { pendingStep =>
        val (step, newState) = setPendingToInFlight(pendingStep, state)
        state.stepRefSubscriber.foreach(_ ! step)
        newState
      }
      .getOrElse(state)

  private def loadAndProcess(sequence: Sequence, state: SequencerState, replyTo: ActorRef[LoadAndProcessResponse])(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] = {
    ctx.self ! StartSequence(replyTo)
    //fixme: swallowing loadSequence errors
    load(sequence, None, state)
  }

  private def goToIdle(state: SequencerState)(implicit ctx: ActorContext[EswSequencerMessage]): Unit = {
    ctx.self ! GoIdle(state)
  }

  private def handleSequenceResponse(
      submitResponse: Future[SubmitResponse],
      state: SequencerState
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): Future[SubmitResponse] = {
    submitResponse.onComplete(_ => goToIdle(state))
    submitResponse.map(CommandResponse.withRunId(state.stepList.runId, _))
  }

  // this method gets called from places where it is already checked that step is in pending status
  private def setPendingToInFlight(
      step: Step,
      state: SequencerState
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): (Step, SequencerState) = {
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
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
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
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): SequencerState =
    updateStepStatus(successResponse, Finished.Success(successResponse), state)

  private[ocs] def updateFailure(
      failureResponse: SubmitResponse,
      state: SequencerState
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): SequencerState =
    updateStepStatus(failureResponse, Finished.Failure(failureResponse), state)

  private def updateStepStatus(
      submitResponse: SubmitResponse,
      stepStatus: StepStatus,
      state: SequencerState
  )(
      implicit ec: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): SequencerState = {
    crm.updateSubCommand(submitResponse)
    val newStepList = state.stepList.updateStatus(submitResponse.runId, stepStatus)
    ctx.self ! UpdateSequencerState(state.copy(stepList = newStepList))
    checkForSequenceCompletion(state)

    if (!state.stepList.isFinished) state.readyToExecuteSubscriber.foreach(_ ! Done)

    state.copy(stepList = newStepList)
  }

  private def checkForSequenceCompletion(state: SequencerState): Unit = if (state.stepList.isFinished) {
    crm.updateSubCommand(Completed(emptyChildId))
  }

  private def shutdown(replyTo: ActorRef[Ok.type])(
      implicit ctx: ActorContext[_]
  ): Behavior[EswSequencerMessage] = {
    import ctx.executionContext

    locationService.unregister(AkkaConnection(componentId))
    script.executeShutdown().onComplete { _ =>
      replyTo ! Ok
      ctx.system.terminate
    }
    Behaviors.stopped[EswSequencerMessage]
  }

  private def updateStepList[T <: EditorError](
      replyTo: ActorRef[Ok.type],
      state: SequencerState,
      stepListResultFunc: => Either[T, StepList]
  )(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): SequencerState = {
    val stepListResult: Either[EditorError, StepList] = stepListResultFunc
    val newStepList                                   = stepListResult.getOrElse(state.stepList) // handle failure
    val newState                                      = state.copy(stepList = newStepList)
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
