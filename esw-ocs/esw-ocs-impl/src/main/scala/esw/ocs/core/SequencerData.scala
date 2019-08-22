package esw.ocs.core

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.models.SequencerState.InProgress
import esw.ocs.api.models.StepStatus.{Finished, InFlight}
import esw.ocs.api.models.messages.SequencerMessages.{GoIdle, Update}
import esw.ocs.api.models.messages._
import esw.ocs.api.models.{SequencerState, Step, StepList}

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

  def startSequence(replyTo: ActorRef[SequenceResponse]): SequencerData = {
    val newState = sendNextPendingStepIfAvailable(this)
    newState.readyToExecuteSubscriber.foreach(_ ! Ok) //fixme: make it none after replying
    updateSequenceInCrmAndHandleFinalResponse(replyTo)
    newState
  }

  def pullNextStep(replyTo: ActorRef[PullNextResult]): SequencerData = {
    val newState = copy(stepRefSubscriber = Some(replyTo))
    sendNextPendingStepIfAvailable(newState)
  }

  def readyToExecuteNext(replyTo: ActorRef[Ok.type], state: SequencerState[SequencerMsg]): SequencerData =
    if (stepList.exists(_.isNotInFlight) && (state == InProgress)) {
      replyTo ! Ok
      copy(readyToExecuteSubscriber = None)
    } else copy(readyToExecuteSubscriber = Some(replyTo))

  def updateStepListResult[T >: Ok.type](replyTo: ActorRef[T], stepListResult: Option[Either[T, StepList]]): SequencerData =
    stepListResult
      .map {
        case Left(error)     => replyTo ! error; this
        case Right(stepList) => updateStepList(replyTo, Some(stepList))
      }
      .getOrElse(this) // This will never happen as this method gets called from inProgress data

  def updateStepList[T >: Ok.type](replyTo: ActorRef[T], stepList: Option[StepList]): SequencerData = {
    val newState = copy(stepList)
    replyTo ! Ok
    checkForSequenceCompletion(newState)
    sendNextPendingStepIfAvailable(newState)
  }

  def updateStepStatus(submitResponse: SubmitResponse): SequencerData = {
    val stepStatus = submitResponse match {
      case submitResponse if CommandResponse.isPositive(submitResponse) => Finished.Success(submitResponse)
      case failureResponse                                              => Finished.Failure(failureResponse)
    }

    crm.updateSubCommand(submitResponse)
    val newStepList = stepList.map(_.updateStatus(submitResponse.runId, stepStatus))
    val newState    = copy(stepList = newStepList)
    checkForSequenceCompletion(newState)
    if (!newState.stepList.exists(_.isFinished)) readyToExecuteSubscriber.foreach(_ ! Ok)
    newState
  }

  private def sendNextPendingStepIfAvailable(data: SequencerData): SequencerData = {
    val maybeState = for {
      ref         <- data.stepRefSubscriber
      pendingStep <- data.stepList.flatMap(_.nextExecutable)
    } yield {
      val (step, newState) = setInFlight(pendingStep)
      ref ! PullNextResult(step)
      newState.copy(stepRefSubscriber = None)
    }

    maybeState.getOrElse(data)
  }

  private def setInFlight(step: Step): (Step, SequencerData) = {
    val inflightStep = step.withStatus(InFlight)
    val newState     = copy(stepList = stepList.map(_.updateStep(inflightStep)))
    updateStepInCrmAndHandleResponse(step.id)
    (inflightStep, newState)
  }

  private def updateSequenceInCrmAndHandleFinalResponse(replyTo: ActorRef[SequenceResponse]): Unit = {
    sequenceId.foreach { id =>
      crm.addOrUpdateCommand(Started(id))
      crm.addSubCommand(id, emptyChildId)
      handleSubmitResponse(
        id,
        crm.queryFinal(id),
        onComplete = response => {
          replyTo ! SequenceResult(response)
          goToIdle()
        }
      )
    }
  }

  private def updateStepInCrmAndHandleResponse(stepId: Id): Unit = {
    sequenceId.foreach { id =>
      crm.addOrUpdateCommand(CommandResponse.Started(stepId))
      crm.addSubCommand(id, stepId)
      val submitResponseF = crm.queryFinal(stepId)
      handleSubmitResponse(stepId, submitResponseF, onComplete = self ! Update(_, actorSystem.deadLetters))
    }
  }

  private def handleSubmitResponse(
      stepId: Id,
      submitResponseF: Future[SubmitResponse],
      onComplete: SubmitResponse => Unit
  ): Unit =
    submitResponseF
      .onComplete {
        case Failure(e)              => onComplete(Error(stepId, e.getMessage))
        case Success(submitResponse) => onComplete(submitResponse)
      }

  private def goToIdle(): Unit = self ! GoIdle(actorSystem.deadLetters)

  private def checkForSequenceCompletion(data: SequencerData): Unit =
    if (data.stepList.exists(_.isFinished)) {
      crm.updateSubCommand(Completed(emptyChildId))
    }
}

object SequencerData {
  def initial(
      self: ActorRef[SequencerMsg],
      crm: CommandResponseManager
  )(implicit actorSystem: ActorSystem[_], timeout: Timeout) =
    SequencerData(None, None, None, self, crm, actorSystem, timeout)
}
