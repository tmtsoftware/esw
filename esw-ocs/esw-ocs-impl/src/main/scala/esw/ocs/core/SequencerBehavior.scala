package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.commands.{CommandResponse, Sequence}
import csw.params.core.models.Id
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.{Unhandled, _}
import esw.ocs.api.models.{SequencerState, Step, StepList}
import esw.ocs.dsl.ScriptDsl
import esw.ocs.internal.Timeouts

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class SequencerBehavior(
    componentId: ComponentId,
    script: ScriptDsl,
    locationService: LocationService,
    crm: CommandResponseManager
)(implicit val actorSystem: ActorSystem[_], timeout: Timeout)
    extends OcsFrameworkCodecs {

  private val emptyChildId = Id("empty-child") // fixme

  //BEHAVIORS
  def idle(sequencerState: SequencerState): Behavior[SequencerMsg] = receive[IdleMessage]("idle") { (ctx, msg) =>
    val state = sequencerState.copy(self = Some(ctx.self))
    import ctx.executionContext
    msg match {
      case x: AnyStateMessage                              => handleAnyStateMessage(x, state, _ => ctx.system.terminate)
      case LoadSequence(sequence, replyTo)                 => load(sequence, replyTo, state)(nextBehavior = loaded)
      case LoadAndStartSequenceInternal(sequence, replyTo) => loadAndStart(sequence, state, replyTo)
      case GoOffline(replyTo)                              => goOffline(replyTo, state)
      case PullNext(replyTo)                               => pullNext(state, replyTo, nextBehavior = idle)
    }
  }

  def loaded(state: SequencerState): Behavior[SequencerMsg] = receive[SequenceLoadedMessage]("loaded") { (ctx, msg) =>
    import ctx.executionContext
    msg match {
      case x: AnyStateMessage         => handleAnyStateMessage(x, state, _ => ctx.system.terminate)
      case editorAction: EditorAction => loaded(handleEditorAction(editorAction, state))
      case GoOffline(replyTo)         => goOffline(replyTo, state)
      case StartSequence(replyTo)     => process(state, replyTo)
    }
  }

  def inProgress(state: SequencerState): Behavior[SequencerMsg] = receive[InProgressMessage]("in-progress") { (ctx, msg) =>
    import ctx.executionContext
    msg match {
      case x: AnyStateMessage          => handleAnyStateMessage(x, state, _ => ctx.system.terminate)
      case editorAction: EditorAction  => inProgress(handleEditorAction(editorAction, state))
      case PullNext(replyTo)           => pullNext(state, replyTo, nextBehavior = inProgress)
      case MaybeNext(replyTo)          => replyTo ! MaybeNextResult(state.stepList.nextExecutable); Behaviors.same
      case ReadyToExecuteNext(replyTo) => readyToExecuteNext(state, replyTo, inProgress)
      case Update(submitResponse, _)   => inProgress(updateStepStatus(submitResponse, state))
      case GoIdle(_) =>
        idle(state) //todo: should clear the state? so that immediate start message does not start the old sequence again?
    }
  }

  private def handleAnyStateMessage(
      message: AnyStateMessage,
      state: SequencerState,
      killFunction: Unit => Unit
  ): Behavior[SequencerMsg] = message match {
    case Shutdown(replyTo)            => shutdown(replyTo, killFunction)
    case GetPreviousSequence(replyTo) => getPreviousSequence(replyTo, state)
  }

  private def handleEditorAction(editorAction: EditorAction, state: SequencerState)(
      implicit ec: ExecutionContext
  ): SequencerState = {
    editorAction match {
      case Abort(replyTo)                     => ??? //story not played yet
      case GetSequence(replyTo)               => getSequence(replyTo, state)
      case Add(commands, replyTo)             => updateStepList1(replyTo, state, state.stepList.append(commands))
      case Pause(replyTo)                     => updateStepList(replyTo, state, state.stepList.pause)
      case Resume(replyTo)                    => updateStepList1(replyTo, state, state.stepList.resume)
      case Reset(replyTo)                     => updateStepList1(replyTo, state, state.stepList.discardPending)
      case Replace(id, commands, replyTo)     => updateStepList(replyTo, state, state.stepList.replace(id, commands))
      case Prepend(commands, replyTo)         => updateStepList1(replyTo, state, state.stepList.prepend(commands))
      case Delete(id, replyTo)                => updateStepList(replyTo, state, state.stepList.delete(id))
      case InsertAfter(id, commands, replyTo) => updateStepList(replyTo, state, state.stepList.insertAfter(id, commands))
      case AddBreakpoint(id, replyTo)         => updateStepList(replyTo, state, state.stepList.addBreakpoint(id))
      case RemoveBreakpoint(id, replyTo)      => updateStepList(replyTo, state, state.stepList.removeBreakpoint(id))
    }
  }

  def offline(state: SequencerState): Behavior[SequencerMsg] = receive[OfflineMessage]("offline") { (ctx, message) =>
    message match {
      case x: AnyStateMessage => handleAnyStateMessage(x, state, _ => ctx.system.terminate)
      case GoOnline(replyTo)  => goOnline(replyTo, state)
    }
  }

  // $COVERAGE-OFF$

  private def pullNext(
      state: SequencerState,
      replyTo: ActorRef[PullNextResult],
      nextBehavior: SequencerState => Behavior[SequencerMsg]
  )(implicit ec: ExecutionContext): Behavior[SequencerMsg] = {
    val newState = state.copy(stepRefSubscriber = Some(replyTo))
    nextBehavior(tryExecutingNextPendingStep(newState))
  }

  private def process(state: SequencerState, replyTo: ActorRef[SequenceResponse])(
      implicit ec: ExecutionContext
  ): Behavior[SequencerMsg] = {
    val id = state.stepList.runId
    crm.addOrUpdateCommand(Started(id))
    crm.addSubCommand(id, emptyChildId)
    val newState = state.stepList.nextExecutable match {
      case Some(step) if step.isPending =>
        val (newStep, newState) = setPendingToInFlight(step, state)
        sendStepToSubscriber(newState, newStep)
      case None => state
    }

    newState.readyToExecuteSubscriber.foreach(_ ! Ok)
    //fixme: this does not handle future failure
    //fixme: this should send message immediately and not after sequence is finished
    handleSequenceResponse(crm.queryFinal(id), newState).foreach(replyTo ! SequenceResult(_))
    inProgress(newState)
  }

  private def sendStepToSubscriber(state: SequencerState, step: Step): SequencerState = {
    state.stepRefSubscriber.foreach(_ ! PullNextResult(step))
    state.copy(stepRefSubscriber = None)
  }

  private def createStepList(
      sequence: Sequence,
      state: SequencerState
  ): Either[DuplicateIdsFound.type, SequencerState] =
    StepList(sequence).map(x => state.copy(stepList = x, previousStepList = Some(state.stepList))) //fixme: make sure previous steplist is none on first time load

  private def load(sequence: Sequence, replyTo: ActorRef[LoadSequenceResponse], state: SequencerState)(
      nextBehavior: SequencerState => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] =
    createStepList(sequence, state) match {
      case Left(err)       => replyTo ! err; Behaviors.same
      case Right(newState) => replyTo ! Ok; nextBehavior(newState)
    }

  private def loadAndStart(
      sequence: Sequence,
      state: SequencerState,
      replyTo: ActorRef[SequenceResponse]
  )(implicit ec: ExecutionContext): Behavior[SequencerMsg] =
    createStepList(sequence, state) match {
      case Left(err)       => replyTo ! err; Behaviors.same
      case Right(newState) => process(newState, replyTo)
    }

  private def receive[B <: SequencerMsg: ClassTag](stateName: String)(
      f: (ActorContext[SequencerMsg], B) => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case msg: B                   => f(ctx, msg)
        case msg: EswSequencerMessage => msg.replyTo ! Unhandled(stateName, msg.getClass.getSimpleName); Behaviors.same
        case LoadAndStartSequence(sequence, replyTo) =>
          import ctx.executionContext
          implicit val timeout: Timeout = Timeouts.LongTimeout
          implicit val scheduler: Scheduler = ctx.system.scheduler

          val sequenceResponseF: Future[SequenceResponse] = ctx.self ? (LoadAndStartSequenceInternal(sequence, _))
          sequenceResponseF.foreach(res => replyTo ! res.toSubmitResponse(sequence.runId))
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }

  private def readyToExecuteNext(
      state: SequencerState,
      replyTo: ActorRef[SimpleResponse],
      behaviour: SequencerState => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = {
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
  ): Behavior[SequencerMsg] = {
    replyTo ! GetPreviousSequenceResult(state.previousStepList)
    Behaviors.same
  }

  private def tryExecutingNextPendingStep(state: SequencerState)(implicit ec: ExecutionContext): SequencerState =
    state.stepList.nextExecutable
      .map { pendingStep =>
        val (step, newState) = setPendingToInFlight(pendingStep, state)
        state.stepRefSubscriber.foreach(_ ! PullNextResult(step))
        newState
      }
      .getOrElse(state)

  private def goToIdle(state: SequencerState): Unit = state.self.foreach(_ ! GoIdle(actorSystem.deadLetters))

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

  private def processStepResponse(stepId: Id, submitResponseF: Future[SubmitResponse], state: SequencerState)(
      implicit ec: ExecutionContext
  ): Unit = {
    def update(submitResponse: SubmitResponse): Unit = state.self.foreach(_ ! Update(submitResponse, actorSystem.deadLetters))

    submitResponseF
      .onComplete {
        case Failure(e)              => update(Error(stepId, e.getMessage))
        case Success(submitResponse) => update(submitResponse)
      }
  }

  private def updateStepStatus(submitResponse: SubmitResponse, state: SequencerState): SequencerState = {
    val stepStatus = submitResponse match {
      case submitResponse if CommandResponse.isPositive(submitResponse) => Finished.Success(submitResponse)
      case failureResponse                                              => Finished.Failure(failureResponse)
    }

    crm.updateSubCommand(submitResponse)
    val newStepList = state.stepList.updateStatus(submitResponse.runId, stepStatus)
    checkForSequenceCompletion(state)

    if (!state.stepList.isFinished) state.readyToExecuteSubscriber.foreach(_ ! Ok)

    state.copy(stepList = newStepList)
  }

  private def checkForSequenceCompletion(state: SequencerState): Unit = if (state.stepList.isFinished) {
    crm.updateSubCommand(Completed(emptyChildId))
  }

  private def shutdown(replyTo: ActorRef[Ok.type], killFunction: Unit => Unit): Behavior[SequencerMsg] = {
    locationService.unregister(AkkaConnection(componentId))
    script.executeShutdown()
    replyTo ! Ok
    Behaviors.stopped(() => killFunction(()))
  }

  private def updateStepList[T >: Ok.type](
      replyTo: ActorRef[T],
      state: SequencerState,
      stepListResult: Either[T, StepList]
  )(implicit ec: ExecutionContext): SequencerState = stepListResult match {
    case Left(error)     => replyTo ! error; state
    case Right(stepList) => updateStepList1(replyTo, state, stepList)
  }

  private def updateStepList1[T >: Ok.type](
      replyTo: ActorRef[T],
      state: SequencerState,
      stepList: StepList
  )(implicit ec: ExecutionContext): SequencerState = {
    val newState = state.copy(stepList)
    replyTo ! Ok
    checkForSequenceCompletion(newState)
    tryExecutingNextPendingStep(newState)
  }

  private def goOnline(replyTo: ActorRef[Ok.type], state: SequencerState): Behavior[SequencerMsg] = {
    replyTo ! Ok
    script.executeGoOnline() // recover and log
    idle(state)
  }

  private def goOffline(replyTo: ActorRef[Ok.type], state: SequencerState): Behavior[SequencerMsg] = {
    replyTo ! Ok
    script.executeGoOffline()                      // recover and log
    offline(state.copy(stepList = StepList.empty)) //fixme: replace with None
  }

  // $COVERAGE-ON$

}
