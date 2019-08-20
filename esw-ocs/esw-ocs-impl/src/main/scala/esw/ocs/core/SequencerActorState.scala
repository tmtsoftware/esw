package esw.ocs.core

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus.{Finished, InFlight}
import esw.ocs.api.models.messages.SequencerMessages.{EswSequencerMessage, GoIdle, Update}
import esw.ocs.api.models.messages._
import esw.ocs.api.models.{Step, StepList}

import scala.concurrent.Future
import scala.util.{Failure, Success}

private[core] case class SequencerActorState(
    stepList: Option[StepList],
    previousStepList: Option[StepList],
    readyToExecuteSubscriber: Option[ActorRef[OkOrUnhandledResponse]],
    stepRefSubscriber: Option[ActorRef[PullNextResult]],
    self: ActorRef[EswSequencerMessage],
    crm: CommandResponseManager,
    actorSystem: ActorSystem[_],
    timeout: Timeout
) {

  implicit private val _timeout: Timeout = timeout

  import actorSystem.executionContext

  private val sequenceId   = stepList.map(_.runId)
  private val emptyChildId = Id("empty-child") // fixme

  def startSequence(replyTo: ActorRef[SequenceResponse]): SequencerActorState = {
    val newState = sendNextPendingStepIfAvailable(this)
    newState.readyToExecuteSubscriber.foreach(_ ! Ok) //fixme: make it none after replying
    updateSequenceInCrmAndHandleFinalResponse(replyTo)
    newState
  }

  def pullNextStep(replyTo: ActorRef[PullNextResult]): SequencerActorState = {
    val newState = copy(stepRefSubscriber = Some(replyTo))
    sendNextPendingStepIfAvailable(newState)
  }

  def readyToExecuteNext(replyTo: ActorRef[OkOrUnhandledResponse]): SequencerActorState =
    if (stepList.exists(_.isInFlight) || stepList.exists(_.isFinished)) {
      copy(readyToExecuteSubscriber = Some(replyTo))
    } else {
      replyTo ! Ok
      copy(readyToExecuteSubscriber = None)
    }

  def updateStepListResult[T >: Ok.type](replyTo: ActorRef[T], stepListResult: Option[Either[T, StepList]]): SequencerActorState =
    stepListResult
      .map {
        case Left(error)     => replyTo ! error; this
        case Right(stepList) => updateStepList(replyTo, Some(stepList))
      }
      .getOrElse(this) // This will never happen as this method gets called from inProgress state

  def updateStepList[T >: Ok.type](replyTo: ActorRef[T], stepList: Option[StepList]): SequencerActorState = {
    val newState = copy(stepList)
    replyTo ! Ok
    checkForSequenceCompletion(newState)
    sendNextPendingStepIfAvailable(newState)
  }

  def updateStepStatus(submitResponse: SubmitResponse): SequencerActorState = {
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

  private def sendNextPendingStepIfAvailable(state: SequencerActorState): SequencerActorState = {
    val maybeState = for {
      ref         <- state.stepRefSubscriber
      pendingStep <- state.stepList.flatMap(_.nextExecutable)
    } yield {
      val (step, newState) = setInFlight(pendingStep)
      ref ! PullNextResult(step)
      newState.copy(stepRefSubscriber = None)
    }

    maybeState.getOrElse(state)
  }

  private def setInFlight(step: Step): (Step, SequencerActorState) = {
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

  private def checkForSequenceCompletion(state: SequencerActorState): Unit =
    if (state.stepList.exists(_.isFinished)) {
      crm.updateSubCommand(Completed(emptyChildId))
    }
}

object SequencerActorState {
  def initial(
      self: ActorRef[EswSequencerMessage],
      crm: CommandResponseManager
  )(implicit actorSystem: ActorSystem[_], timeout: Timeout) =
    SequencerActorState(None, None, None, None, self, crm, actorSystem, timeout)
}
