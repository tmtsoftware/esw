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
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.models.messages.EditorError.UpdateError
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.SequencerResponses.{EditorResponse, LifecycleResponse, LoadSequenceResponse, StepListResponse}
import esw.ocs.api.models.messages.ShutdownError
import esw.ocs.api.models.{SequencerState, Step, StepList, StepStatus}
import esw.ocs.dsl.ScriptDsl

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.control.NonFatal

class SequencerBehavior(
    componentId: ComponentId,
    sequencer: Sequencer,
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
      case Shutdown(replyTo)  => shutdown(replyTo, ctx)
      case GoOffline(replyTo) => goOffline(replyTo, state)

      // ===== External Editor =====
      case LoadSequence(sequence, replyTo)   => load(sequence, Some(replyTo), state)
      case LoadAndProcess(sequence, replyTo) => loadAndProcess(sequence, state, replyTo)
      case GetPreviousSequence(replyTo) =>
        replyTo ! StepListResponse(state.previousStepList)
        Behaviors.same
      // ===== Internal =====
      case PullNext(replyTo) => pullNext(state, replyTo, idle)
    }
  }

  def loaded(state: SequencerState): Behavior[EswSequencerMessage] = receive[SequenceLoadedMessage]("loaded") { (ctx, msg) =>
    import ctx._
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo)      => shutdown(replyTo, ctx)
      case GoOffline(replyTo)     => goOffline(replyTo, state)
      case StartSequence(replyTo) => process(state, replyTo)
//        sequencer.start().foreach(replyTo.tell); Behaviors.same

      // ===== External Editor =====
      case Abort(replyTo)               => ??? //story not played yet
      case Available(replyTo)           => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetSequence(replyTo)         => sequencer.getSequence.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo) => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same
      case Add(commands, replyTo)       => sequencer.add(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Pause(replyTo)               => sequencer.pause.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Resume(replyTo)              => sequencer.resume.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Reset(replyTo)               => sequencer.reset().foreach(replyTo ! EditorResponse(_)); idle
      case Replace(id, commands, replyTo) =>
        sequencer.replace(id, commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Prepend(commands, replyTo) => sequencer.prepend(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Delete(id, replyTo)        => sequencer.delete(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case InsertAfter(id, cmds, replyTo) =>
        sequencer.insertAfter(id, cmds).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case AddBreakpoint(id, replyTo) => sequencer.addBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case RemoveBreakpoint(id, replyTo) => sequencer.removeBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo) => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
    }
  }

  def inProgress(state: SequencerState): Behavior[EswSequencerMessage] = receive[InProgressMessage]("in-progress") { (ctx, msg) =>
    msg match {
      // ===== External Lifecycle =====
      case Shutdown(replyTo) => shutdown(replyTo, ctx); Behaviors.same
      case Abort(replyTo) => abort(replyTo); Behaviors.same

      // ===== External Editor =====
      case Available(replyTo)             => sequencer.isAvailable.foreach(replyTo.tell); Behaviors.same
      case GetSequence(replyTo)           => sequencer.getSequence.foreach(replyTo.tell); Behaviors.same
      case GetPreviousSequence(replyTo)   => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_)); Behaviors.same
      case Add(commands, replyTo)         => sequencer.add(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Pause(replyTo)                 => sequencer.pause.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Resume(replyTo)                => sequencer.resume.foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Reset(replyTo)                 => sequencer.reset().foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Replace(id, commands, replyTo) => sequencer.replace(id, commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Prepend(commands, replyTo)     => sequencer.prepend(commands).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case Delete(id, replyTo)            => sequencer.delete(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case InsertAfter(id, cmds, replyTo) => sequencer.insertAfter(id, cmds).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case AddBreakpoint(id, replyTo)     => sequencer.addBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same
      case RemoveBreakpoint(id, replyTo) => sequencer.removeBreakpoint(id).foreach(replyTo ! EditorResponse(_)); Behaviors.same

      // ===== Internal =====
      case PullNext(replyTo)              => sequencer.pullNext().foreach(replyTo.tell); Behaviors.same
      case MaybeNext(replyTo)             => sequencer.mayBeNext.foreach(replyTo.tell); Behaviors.same
      case ReadyToExecuteNext(replyTo)    => sequencer.readyToExecuteNext().foreach(replyTo.tell); Behaviors.same
      case UpdateFailure(failureResponse) => sequencer.updateFailure(failureResponse); Behaviors.same
      case UpdateSequencerState(newState) => inProgress(newState)
    }
  }

  def offline(state: SequencerState): Behavior[EswSequencerMessage] = receive[OfflineMessage]("offline") { (context, message) =>
    message match {
      case GoOnline(replyTo)            => goOnline().foreach(replyTo.tell); idle
      case Shutdown(replyTo)            => shutdown(replyTo)(context); Behaviors.stopped
      case GetPreviousSequence(replyTo) => replyTo ! StepListResponse(None); Behaviors.same
    }
  }

  // $COVERAGE-OFF$

  def pullNext(state: SequencerState, replyTo: ActorRef[Step], behaviour: SequencerState => Behavior[EswSequencerMessage])(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] = {
    val (stepF, newState) = state.stepList.nextExecutable match {
      // step.isPending check is actually not required, but kept here in case impl of nextExecutable gets changed
      case Some(step) if step.isPending =>
        val (step, newState) = setPendingToInFlight(step, state)
        (Future.successful(step), newState)
      case None => createStepRefPromise(state)
    }
    stepF.foreach(replyTo.tell)
    behaviour(newState)
  }

  def createStepRefPromise(state: SequencerState): (Future[Step], SequencerState) = {
    val stepPromise = Promise[Step]()
    val newState    = state.copy(stepRefPromise = Some(stepPromise))
    (stepPromise.future, newState)
  }

//  private def createPromise[T](update: Promise[T] => Unit): Future[T] = {
//    val p = Promise[T]()
//    update(p)
//    p.future
//  }

  private def loadAndProcess(sequence: Sequence, state: SequencerState, replyTo: ActorRef[SubmitResponse])(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] = {
    ctx.self ! StartSequence(replyTo)
    //fixme: swallowing loadSequence errors
    load(sequence, None, state)
  }

  def process(state: SequencerState, replyTo: ActorRef[SubmitResponse])(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Behavior[EswSequencerMessage] = {
    val id = state.stepList.runId
    crm.addOrUpdateCommand(Started(id))
    crm.addSubCommand(id, emptyChildId)
    val newState = completeStepRefPromise(state)
    newState
      .completeReadyToExecuteNextPromise() // To complete the promise created for previous sequence so that engine can pullNext
    //fixme: this does not handle future failure
    //fixme: this should send message immediately and not after sequence is finished
    handleSequenceResponse(crm.queryFinal(id), newState).foreach(replyTo.tell)
    inProgress(newState)
  }

  private def goToIdle(state: SequencerState)(implicit ctx: ActorContext[EswSequencerMessage]): Unit = {
    ctx.self ! GoIdle(state)
  }

  private def handleSequenceResponse(
      submitResponse: Future[SubmitResponse],
      state: SequencerState
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): Future[SubmitResponse] = {
    submitResponse.onComplete(_ => {
      val newState = state.failStepRefPromise()
      goToIdle(newState)
    })
    submitResponse.map(CommandResponse.withRunId(state.stepList.runId, _))
  }

  // this method gets called from places where it is already checked that step is in pending status
  private def setPendingToInFlight(
      step: Step,
      state: SequencerState
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): (Step, SequencerState) = {
    val inflightStep = step.withStatus(Pending, InFlight)
    val newState     = state.copy(stepList = state.stepList.updateStep(inflightStep))
    val stepRunId    = step.id
    crm.addSubCommand(newState.stepList.runId, stepRunId)
    crm.addOrUpdateCommand(CommandResponse.Started(stepRunId))
    processStepResponse(stepRunId, crm.queryFinal(stepRunId), newState)
    (inflightStep, newState)
  }

  private def completeStepRefPromise(
      state: SequencerState
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): SequencerState =
    (for {
      ref  <- state.stepRefPromise
      step <- state.stepList.nextExecutable
      if step.isPending
    } yield {
      val (step, newState) = setPendingToInFlight(step, state)
      ref.complete(Try(step))
      newState.copy(stepRefPromise = None)
    }).getOrElse(state)

  private def processStepResponse(stepId: Id, submitResponseF: Future[SubmitResponse], state: SequencerState)(
      implicit executionContext: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Future[Either[UpdateError, StepList]] =
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
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): Either[UpdateError, StepList] =
    updateStepStatus(successResponse, Finished.Success(successResponse), state)

  private[ocs] def updateFailure(
      failureResponse: SubmitResponse,
      state: SequencerState
  )(implicit ec: ExecutionContext, ctx: ActorContext[EswSequencerMessage]): Either[UpdateError, StepList] =
    updateStepStatus(failureResponse, Finished.Failure(failureResponse), state)

  private def updateStepStatus(submitResponse: SubmitResponse, stepStatus: StepStatus, state: SequencerState)(
      implicit ec: ExecutionContext,
      ctx: ActorContext[EswSequencerMessage]
  ): Either[UpdateError, StepList] = {
    crm.updateSubCommand(submitResponse)
    val updateStatusResult = state.stepList.updateStatus(submitResponse.runId, stepStatus)
    updateStatusResult.foreach { _stepList =>
      ctx.self ! UpdateSequencerState(state.copy(stepList = _stepList))
      checkForSequenceCompletion(state)
      state.completeReadyToExecuteNextPromise()
    }
    updateStatusResult
  }

  private def checkForSequenceCompletion(state: SequencerState): Unit = if (state.stepList.isFinished) {
    crm.updateSubCommand(Completed(emptyChildId))
  }

  def load(
      sequence: Sequence,
      replyTo: Option[ActorRef[LoadSequenceResponse]],
      state: SequencerState
  ): Behavior[EswSequencerMessage] = {
    StepList(sequence) match {
      case Left(err) =>
        replyTo.foreach(_ ! LoadSequenceResponse(Left(err)))
        Behaviors.same
      case Right(x) =>
        replyTo.foreach(_ ! LoadSequenceResponse(Right(Done)))
        loaded(state.copy(stepList = x, previousStepList = Some(state.stepList)))
    }
  }

  private def shutdown(replyTo: ActorRef[LifecycleResponse], ctx: ActorContext[_]): Behavior[EswSequencerMessage] = {
    //todo: this blocking is temporary and will go away when shutdown story is played
    Try {
      Await.result(
        locationService
          .unregister(AkkaConnection(componentId)),
        atMost
      )
      Try {
        Await.result(script.executeShutdown(), atMost) //todo: log this
      }
      replyTo ! LifecycleResponse(Right(Done))
      //fixme : this is not safe. not sure of previous message is sent yet.
      // to be looked at in shutdown story
      ctx.system.terminate
      Behaviors.stopped[EswSequencerMessage]
    }.recover {
      case NonFatal(err) =>
        replyTo ! LifecycleResponse(
          Left(
            ShutdownError(
              "could not unregister sequencer\n" +
                err.getMessage
            )
          )
        )
        Behaviors.same[EswSequencerMessage]
    }.get
  }

//  def reset(state: SequencerState): Future[Either[ResetError, Done]] = updateStepListResult(state.stepList.discardPending)

//  // stepListResultFunc is by name because all StepList operations must execute on strandEc
//  private def updateStepListResult[T <: EditorError](stepListResultFunc: => Either[T, StepList]) = {
//    val stepListResult = stepListResultFunc
//    stepListResult.map { s =>
//      stepList = s
//      checkForSequenceCompletion(state)
//      completeStepRefPromise()
//      Done
//    }
//  }

  protected def receive[B <: EswSequencerMessage: ClassTag](
      stateName: String
  )(f: (ActorContext[EswSequencerMessage], B) => Behavior[EswSequencerMessage]): Behavior[EswSequencerMessage] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case m: B => f(ctx, m)
        case _    =>
          //fixme: handle the Unhandled
          //m.replyTo ! Unhandled(stateName, m.getClass.getSimpleName)
          Behaviors.same
      }
    }

  //HANDLERS
  private def goOnline()(implicit ec: ExecutionContext): Future[LifecycleResponse] =
    script.executeGoOnline() // recover and log
  LifecycleResponse(Right(Done))

  private def goOffline(replyTo: ActorRef[LifecycleResponse], state: SequencerState): Behavior[EswSequencerMessage] = {
    replyTo ! LifecycleResponse(Right(Done))
    script.executeGoOffline() // recover and log
    offline(SequencerState.initial.copy(state.stepList))
  }

  // $COVERAGE-ON$

}
