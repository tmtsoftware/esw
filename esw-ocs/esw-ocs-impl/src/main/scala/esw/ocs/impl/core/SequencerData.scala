package esw.ocs.impl.core

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.commands.{CommandResponse, Sequence}
import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus.{Finished, InFlight}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol.{DuplicateIdsFound, Ok, PullNextResult, SequenceResponse, SequenceResult}
import esw.ocs.impl.messages.SequencerMessages.{GoIdle, Update}
import esw.ocs.impl.messages.SequencerState
import esw.ocs.impl.messages.SequencerState.{InProgress, Loaded}

import scala.concurrent.Future
import scala.util.{Failure, Success}

private[core] case class SequencerData(
    stepList: Option[StepList],
    readyToExecuteSubscriber: Option[ActorRef[Ok.type]],
    stepRefSubscriber: Option[ActorRef[PullNextResult]],
    self: ActorRef[SequencerMsg],
    crm: CommandResponseManager,
    actorSystem: ActorSystem[_],
    timeout: Timeout
) {

  implicit private val _timeout: Timeout = timeout

  import actorSystem.executionContext

  private val sequenceId   = stepList.map(_.runId)
  private val emptyChildId = Id("empty-child") // fixme

  def createStepList(sequence: Sequence): Either[DuplicateIdsFound.type, SequencerData] =
    StepList(sequence).map(currentStepList => copy(stepList = Some(currentStepList)))

  def startSequence(replyTo: ActorRef[Ok.type]): SequencerData = {
    replyTo ! Ok
    processSequence(onComplete = _ => goToIdle())
  }

  def processSequence(onComplete: SubmitResponse => Unit): SequencerData =
    sendNextPendingStepIfAvailable()
      .notifyReadyToExecuteNextSubscriber(InProgress)
      .updateSequenceInCrmAndHandleFinalResponse(onComplete)

  def querySequence(replyTo: ActorRef[SequenceResponse]): SequencerData = {
    sequenceId.foreach(id => crm.queryFinal(id).foreach(replyTo ! SequenceResult(_)))
    this
  }

  def pullNextStep(replyTo: ActorRef[PullNextResult]): SequencerData =
    copy(stepRefSubscriber = Some(replyTo))
      .sendNextPendingStepIfAvailable()

  def readyToExecuteNext(replyTo: ActorRef[Ok.type], state: SequencerState[SequencerMsg]): SequencerData =
    if (stepList.exists(_.isRunningButNotInFlight) && (state == InProgress)) {
      replyTo ! Ok
      copy(readyToExecuteSubscriber = None)
    }
    else copy(readyToExecuteSubscriber = Some(replyTo))

  def updateStepListResult[T >: Ok.type](
      replyTo: ActorRef[T],
      state: SequencerState[SequencerMsg],
      stepListResult: Option[Either[T, StepList]]
  ): SequencerData =
    stepListResult
      .map {
        case Left(error)     => replyTo ! error; this
        case Right(stepList) => updateStepList(replyTo, state, Some(stepList))
      }
      .getOrElse(this) // This will never happen as this method gets called from inProgress data

  def updateStepList[T >: Ok.type](
      replyTo: ActorRef[T],
      state: SequencerState[SequencerMsg],
      stepList: Option[StepList]
  ): SequencerData = {
    replyTo ! Ok
    val updatedData = copy(stepList)
    if (state == Loaded) updatedData
    else
      updatedData
        .checkForSequenceCompletion()
        .notifyReadyToExecuteNextSubscriber(state)
        .sendNextPendingStepIfAvailable()
  }

  def updateStepStatus(submitResponse: SubmitResponse, state: SequencerState[SequencerMsg]): SequencerData = {
    val stepStatus = submitResponse match {
      case submitResponse if CommandResponse.isPositive(submitResponse) => Finished.Success(submitResponse)
      case failureResponse                                              => Finished.Failure(failureResponse)
    }

    crm.updateSubCommand(submitResponse)
    val newStepList = stepList.map(_.updateStatus(submitResponse.runId, stepStatus))

    copy(stepList = newStepList)
      .checkForSequenceCompletion()
      .notifyReadyToExecuteNextSubscriber(state)
  }

  private def sendNextPendingStepIfAvailable(): SequencerData = {
    val maybeData = for {
      ref         <- stepRefSubscriber
      pendingStep <- stepList.flatMap(_.nextExecutable)
    } yield {
      val (step, updatedData) = setInFlight(pendingStep)
      ref ! PullNextResult(step)
      updatedData.copy(stepRefSubscriber = None)
    }

    maybeData.getOrElse(this)
  }

  private def setInFlight(step: Step): (Step, SequencerData) = {
    val inflightStep = step.withStatus(InFlight)
    val updatedData  = copy(stepList = stepList.map(_.updateStep(inflightStep)))
    updateStepInCrmAndHandleResponse(step.id)
    (inflightStep, updatedData)
  }

  private def updateSequenceInCrmAndHandleFinalResponse(
      onComplete: SubmitResponse => Unit
  ): SequencerData = {
    sequenceId.foreach { id =>
      crm.addOrUpdateCommand(Started(id))
      crm.addSubCommand(id, emptyChildId)
      val sequenceResponseF = crm.queryFinal(id)
      handleSubmitResponse(id, sequenceResponseF, onComplete)
    }
    this
  }

  private def updateStepInCrmAndHandleResponse(stepId: Id): Unit =
    sequenceId.foreach { id =>
      crm.addOrUpdateCommand(CommandResponse.Started(stepId))
      crm.addSubCommand(id, stepId)
      val stepResponseF = crm.queryFinal(stepId)
      handleSubmitResponse(stepId, stepResponseF, onComplete = self ! Update(_, actorSystem.deadLetters))
    }

  private def handleSubmitResponse(
      id: Id,
      submitResponseF: Future[SubmitResponse],
      onComplete: SubmitResponse => Unit
  ): Unit = submitResponseF.onComplete {
    case Failure(e)              => onComplete(Error(id, e.getMessage))
    case Success(submitResponse) => onComplete(submitResponse)
  }

  def goToIdle(): Unit = self ! GoIdle(actorSystem.deadLetters)

  private def checkForSequenceCompletion(): SequencerData = {
    if (stepList.exists(_.isFinished)) {
      crm.updateSubCommand(Completed(emptyChildId))
    }
    this
  }

  private def notifyReadyToExecuteNextSubscriber(state: SequencerState[SequencerMsg]): SequencerData =
    readyToExecuteSubscriber.map(replyTo => readyToExecuteNext(replyTo, state)).getOrElse(this)
}

private[core] object SequencerData {
  def initial(self: ActorRef[SequencerMsg], crm: CommandResponseManager)(
      implicit actorSystem: ActorSystem[_],
      timeout: Timeout
  ): SequencerData = SequencerData(None, None, None, self, crm, actorSystem, timeout)
}
