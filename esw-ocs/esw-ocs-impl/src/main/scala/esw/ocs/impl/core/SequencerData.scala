package esw.ocs.impl.core

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.Sequence
import csw.params.core.models.Id
import esw.ocs.api.actor.messages.InternalSequencerState
import esw.ocs.api.actor.messages.SequencerMessages.GoIdle
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.{Finished, InFlight}
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.api.protocol.SequencerStateSubscriptionResponse.SequencerShuttingDown
import esw.ocs.api.protocol._

private[core] case class SequencerData(
    stepList: Option[StepList],
    runId: Option[Id],
    readyToExecuteSubscriber: Option[ActorRef[Ok.type]],
    stepRefSubscriber: Option[ActorRef[PullNextResult]],
    self: ActorRef[SequencerMsg],
    actorSystem: ActorSystem[_],
    sequenceResponseSubscribers: Set[ActorRef[SubmitResponse]],
    sequencerStateSubscribers: Set[ActorRef[SequencerStateSubscriptionResponse]]
) {

  def isSequenceLoaded: Boolean = stepList.isDefined

  def createStepList(sequence: Sequence): SequencerData =
    copy(stepList = Some(StepList(sequence)))

  def startSequence(replyTo: ActorRef[SubmitResult]): SequencerData = {
    val runId = Id()
    replyTo ! SubmitResult(Started(runId))
    copy(runId = Some(runId))
      .processSequence()
  }

  def processSequence(): SequencerData =
    sendNextPendingStepIfAvailable()
      .notifyReadyToExecuteNextSubscriber()

  def queryFinal(runId: Id, replyTo: ActorRef[SubmitResponse]): SequencerData =
    this.runId match {
      case Some(`runId`) if stepList.get.isFinished => replyTo ! getSequencerResponse; this
      case Some(`runId`)                            => copy(sequenceResponseSubscribers = sequenceResponseSubscribers + replyTo)
      case _                                        => sendInvalidResponse(runId, replyTo)
    }

  def query(runId: Id, replyTo: ActorRef[SubmitResponse]): SequencerData = {
    this.runId match {
      case Some(`runId`) if stepList.get.isFinished => replyTo ! getSequencerResponse
      case Some(`runId`)                            => replyTo ! Started(runId)
      case _                                        => sendInvalidResponse(runId, replyTo)
    }
    this
  }

  def pullNextStep(replyTo: ActorRef[PullNextResult]): SequencerData =
    copy(stepRefSubscriber = Some(replyTo))
      .sendNextPendingStepIfAvailable()

  def readyToExecuteNext(replyTo: ActorRef[Ok.type]): SequencerData =
    if (stepList.exists(_.isRunningButNotInFlight) && runId.isDefined) {
      replyTo ! Ok
      copy(readyToExecuteSubscriber = None)
    }
    else copy(readyToExecuteSubscriber = Some(replyTo))

  def updateStepListResult[T >: Ok.type](
      replyTo: ActorRef[T],
      stepListResult: Option[Either[T, StepList]]
  ): SequencerData =
    stepListResult
      .map {
        case Left(error)     => replyTo ! error; this
        case Right(stepList) => updateStepList(replyTo, Some(stepList))
      }
      .getOrElse(this) // This will never happen as this method gets called from inProgress data

  def updateStepList[T >: Ok.type](
      replyTo: ActorRef[T],
      stepList: Option[StepList]
  ): SequencerData = {
    replyTo ! Ok
    val updatedData = copy(stepList)
    if (runId.isEmpty) updatedData
    else
      updatedData
        .checkForSequenceCompletion()
        .notifyReadyToExecuteNextSubscriber()
        .sendNextPendingStepIfAvailable()
  }

  def stepSuccess(state: InternalSequencerState[SequencerMsg]): SequencerData = changeStepStatus(Success)

  def stepFailure(message: String, state: InternalSequencerState[SequencerMsg]): SequencerData =
    changeStepStatus(Failure(message))

  def addStateSubscriber(subscriber: ActorRef[SequencerStateSubscriptionResponse]): SequencerData =
    copy(sequencerStateSubscribers = sequencerStateSubscribers + subscriber)

  def removeStateSubscriber(subscriber: ActorRef[SequencerStateSubscriptionResponse]): SequencerData =
    copy(sequencerStateSubscribers = sequencerStateSubscribers - subscriber)

  def notifyStateChange(state: InternalSequencerState[SequencerMsg]): SequencerData = {
    sequencerStateSubscribers.foreach(_ ! SequencerStateResponse(stepList.getOrElse(StepList.empty), state.toExternal))
    this
  }

  def notifySequencerShutdown(): Unit = sequencerStateSubscribers.foreach(x => x ! SequencerShuttingDown)

  private def sendInvalidResponse(runId: Id, replyTo: ActorRef[SubmitResponse]): SequencerData = {
    replyTo ! Invalid(runId, IdNotAvailableIssue(s"Sequencer is not running any sequence with runId $runId"))
    this
  }

  private def changeStepStatus(newStatus: StepStatus) = {
    val newStepList = stepList.map(stepList => StepList(stepList.steps.map(_.withStatus(newStatus))))
    copy(stepList = newStepList)
      .checkForSequenceCompletion()
      .notifyReadyToExecuteNextSubscriber()
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
    (inflightStep, updatedData)
  }

  private def getSequencerResponse: SubmitResponse =
    stepList
      .flatMap {
        _.steps.collectFirst { case s @ Step(_, _, Finished.Failure(message), _) =>
          Error(runId.get, s"${s.info}, reason: $message")
        }
      }
      .getOrElse(Completed(runId.get))

  private def checkForSequenceCompletion(): SequencerData =
    if (stepList.exists(_.isFinished)) {
      val submitResponse = getSequencerResponse
      sequenceResponseSubscribers.foreach(_ ! submitResponse)
      self ! GoIdle(actorSystem.deadLetters)
      copy(sequenceResponseSubscribers = Set.empty)
    }
    else this

  private def notifyReadyToExecuteNextSubscriber(): SequencerData =
    readyToExecuteSubscriber.map(replyTo => readyToExecuteNext(replyTo)).getOrElse(this)
}

private[core] object SequencerData {
  def initial(self: ActorRef[SequencerMsg])(implicit actorSystem: ActorSystem[_]): SequencerData =
    SequencerData(None, None, None, None, self, actorSystem, Set.empty, Set.empty)
}
