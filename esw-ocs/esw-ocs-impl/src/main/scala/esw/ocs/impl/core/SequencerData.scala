package esw.ocs.impl.core

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.commands.Sequence
import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.{Finished, InFlight}
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.api.protocol._
import esw.ocs.impl.messages.SequencerMessages.GoIdle
import esw.ocs.impl.messages.SequencerState
import esw.ocs.impl.messages.SequencerState.{InProgress, Loaded}

private[core] case class SequencerData(
    stepList: Option[StepList],
    readyToExecuteSubscriber: Option[ActorRef[Ok.type]],
    stepRefSubscriber: Option[ActorRef[PullNextResult]],
    self: ActorRef[SequencerMsg],
    actorSystem: ActorSystem[_],
    sequenceResponseSubscribers: Set[ActorRef[SequenceResponse]]
) {

  private val sequenceId = stepList.map(_.runId)

  def createStepList(sequence: Sequence): SequencerData =
    copy(stepList = Some(StepList(sequence)))

  def startSequence(replyTo: ActorRef[SequenceResult]): SequencerData = {
    replyTo ! SequenceResult(Started(sequenceId.get))
    processSequence()
  }

  def processSequence(): SequencerData =
    sendNextPendingStepIfAvailable()
      .notifyReadyToExecuteNextSubscriber(InProgress)

  def queryFinal(replyTo: ActorRef[SequenceResponse]): SequencerData =
    stepList match {
      case Some(stepList) if stepList.isFinished => replyTo ! SequenceResult(getSequencerResponse); this
      case Some(_)                               => copy(sequenceResponseSubscribers = sequenceResponseSubscribers + replyTo)
      case None                                  => replyTo ! SequenceResult(Error(Id("IdNotAvailable"), "No sequence is loaded in the sequencer")); this
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

  private def changeStepStatus(state: SequencerState[SequencerMsg], newStatus: StepStatus) = {
    val newStepList = stepList.map { stepList =>
      stepList.copy(steps = stepList.steps.map {
        case step if step.isInFlight => step.withStatus(newStatus)
        case x                       => x
      })
    }
    copy(stepList = newStepList)
      .checkForSequenceCompletion()
      .notifyReadyToExecuteNextSubscriber(state)
  }

  def stepSuccess(state: SequencerState[SequencerMsg]): SequencerData =
    changeStepStatus(state, Success)

  def stepFailure(message: String, state: SequencerState[SequencerMsg]): SequencerData =
    changeStepStatus(state, Failure(message))

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
    (inflightStep, updatedData)
  }

  private def getSequencerResponse: SubmitResponse =
    stepList
      .flatMap {
        _.steps.map(_.status).collectFirst {
          case Finished.Failure(message) => Error(sequenceId.get, message)
        }
      }
      .getOrElse(Completed(sequenceId.get))

  private def checkForSequenceCompletion(): SequencerData =
    if (stepList.exists(_.isFinished)) {
      val submitResponse = getSequencerResponse
      sequenceResponseSubscribers.foreach(_ ! SequenceResult(submitResponse))
      self ! GoIdle(actorSystem.deadLetters)
      copy(sequenceResponseSubscribers = Set.empty)
    }
    else this

  private def notifyReadyToExecuteNextSubscriber(state: SequencerState[SequencerMsg]): SequencerData =
    readyToExecuteSubscriber.map(replyTo => readyToExecuteNext(replyTo, state)).getOrElse(this)
}

private[core] object SequencerData {
  def initial(self: ActorRef[SequencerMsg])(implicit actorSystem: ActorSystem[_]): SequencerData =
    SequencerData(None, None, None, self, actorSystem, Set.empty)
}
