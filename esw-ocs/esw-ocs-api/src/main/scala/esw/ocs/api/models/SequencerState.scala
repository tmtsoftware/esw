package esw.ocs.api.models

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus.{Finished, InFlight}
import esw.ocs.api.models.messages.SequencerMessages.{EswSequencerMessage, GoIdle, Update}
import esw.ocs.api.models.messages._

import scala.concurrent.Future
import scala.util.{Failure, Success}

//todo: make steplist option
case class SequencerState(
    stepList: StepList,
    previousStepList: Option[StepList],
    readyToExecuteSubscriber: Option[ActorRef[SimpleResponse]],
    stepRefSubscriber: Option[ActorRef[PullNextResult]],
    self: ActorRef[EswSequencerMessage],
    crm: CommandResponseManager,
    actorSystem: ActorSystem[_],
    timeout: Timeout
) {

  implicit private val _timeout: Timeout = timeout

  import actorSystem.executionContext

  private val sequenceId   = stepList.runId
  private val emptyChildId = Id("empty-child") // fixme

  def startSequence(replyTo: ActorRef[SequenceResponse]): SequencerState = {
    val newState = stepList.nextExecutable match {
      case Some(step) =>
        val (newStep, newState) = setInFlight(step)
        sendStepToSubscriber(newState, newStep)
      case None => this
    }
    newState.readyToExecuteSubscriber.foreach(_ ! Ok)
    updateSequenceInCrmAndHandleFinalResponse(replyTo)
    newState
  }

  def pullNextStep(replyTo: ActorRef[PullNextResult]): SequencerState = {
    val newState = copy(stepRefSubscriber = Some(replyTo))
    sendNextPendingStepIfAvailable(newState)
  }

  def readyToExecuteNext(replyTo: ActorRef[SimpleResponse]): SequencerState =
    if (stepList.isInFlight || stepList.isFinished) {
      copy(readyToExecuteSubscriber = Some(replyTo))
    } else {
      replyTo ! Ok
      copy(readyToExecuteSubscriber = None)
    }

  def updateStepListResult[T >: Ok.type](replyTo: ActorRef[T], stepListResult: Either[T, StepList]): SequencerState =
    stepListResult match {
      case Left(error)     => replyTo ! error; this
      case Right(stepList) => updateStepList(replyTo, stepList)
    }

  def updateStepList[T >: Ok.type](replyTo: ActorRef[T], stepList: StepList): SequencerState = {
    val newState = copy(stepList)
    replyTo ! Ok
    checkForSequenceCompletion(newState)
    sendNextPendingStepIfAvailable(newState)
  }

  def updateStepStatus(submitResponse: SubmitResponse): SequencerState = {
    val stepStatus = submitResponse match {
      case submitResponse if CommandResponse.isPositive(submitResponse) => Finished.Success(submitResponse)
      case failureResponse                                              => Finished.Failure(failureResponse)
    }

    crm.updateSubCommand(submitResponse)
    val newStepList = stepList.updateStatus(submitResponse.runId, stepStatus)
    val newState    = copy(stepList = newStepList)
    checkForSequenceCompletion(newState)
    if (!newState.stepList.isFinished) readyToExecuteSubscriber.foreach(_ ! Ok)
    newState
  }

  private def sendNextPendingStepIfAvailable(state: SequencerState): SequencerState = {
    val maybeState = for {
      ref         <- state.stepRefSubscriber
      pendingStep <- state.stepList.nextExecutable
    } yield {
      val (step, newState) = setInFlight(pendingStep)
      ref ! PullNextResult(step)
      newState.copy(stepRefSubscriber = None)
    }

    maybeState.getOrElse(state)
  }

  private def setInFlight(step: Step): (Step, SequencerState) = {
    val inflightStep = step.withStatus(InFlight)
    val newState     = copy(stepList = stepList.updateStep(inflightStep))
    updateStepInCrmAndHandleResponse(step.id)
    (inflightStep, newState)
  }

  private def updateSequenceInCrmAndHandleFinalResponse(replyTo: ActorRef[SequenceResponse]): Unit = {
    crm.addOrUpdateCommand(Started(sequenceId))
    crm.addSubCommand(sequenceId, emptyChildId)
    handleFinalResponse(crm.queryFinal(sequenceId), replyTo)
  }

  private def updateStepInCrmAndHandleResponse(stepId: Id): Unit = {
    crm.addOrUpdateCommand(CommandResponse.Started(stepId))
    crm.addSubCommand(sequenceId, stepId)
    handleStepResponse(stepId, crm.queryFinal(stepId))
  }

  private def handleStepResponse(stepId: Id, submitResponseF: Future[SubmitResponse]): Unit = {
    def sendUpdateMsg(submitResponse: SubmitResponse): Unit = selfMessage(Update(submitResponse, actorSystem.deadLetters))

    submitResponseF
      .onComplete {
        case Failure(e)              => sendUpdateMsg(Error(stepId, e.getMessage))
        case Success(submitResponse) => sendUpdateMsg(submitResponse)
      }
  }

  private def handleFinalResponse(submitResponseF: Future[SubmitResponse], replyTo: ActorRef[SequenceResponse]): Unit = {
    def sendSequenceResult(submitResponse: SubmitResponse): Unit = replyTo ! SequenceResult(submitResponse)

    submitResponseF.onComplete { res =>
      res match {
        case Failure(e)              => sendSequenceResult(Error(sequenceId, e.getMessage))
        case Success(submitResponse) => sendSequenceResult(CommandResponse.withRunId(sequenceId, submitResponse))
      }
      goToIdle()
    }
  }

  private def goToIdle(): Unit = selfMessage(GoIdle(actorSystem.deadLetters))

  private def sendStepToSubscriber(state: SequencerState, step: Step): SequencerState = {
    state.stepRefSubscriber.foreach(_ ! PullNextResult(step))
    state.copy(stepRefSubscriber = None)
  }

  private def checkForSequenceCompletion(state: SequencerState): Unit =
    if (state.stepList.isFinished) {
      crm.updateSubCommand(Completed(emptyChildId))
    }

  private def selfMessage(msg: EswSequencerMessage): Unit = self ! msg
}

object SequencerState {
  def initial(
      self: ActorRef[EswSequencerMessage],
      crm: CommandResponseManager
  )(implicit actorSystem: ActorSystem[_], timeout: Timeout) =
    SequencerState(StepList.empty, None, None, None, self, crm, actorSystem, timeout)
}
