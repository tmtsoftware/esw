package esw.ocs.api.models

import akka.actor.typed.ActorRef
import esw.ocs.api.models.messages.SequencerMessages.EswSequencerMessage
import esw.ocs.api.models.messages.{PullNextResult, ReadyToExecuteNextResponse}

//todo: make steplist option
case class SequencerState(
    stepList: StepList,
    previousStepList: Option[StepList],
    readyToExecuteSubscriber: Option[ActorRef[ReadyToExecuteNextResponse]],
    stepRefSubscriber: Option[ActorRef[PullNextResult]],
    self: Option[ActorRef[EswSequencerMessage]]
)

object SequencerState {
  def initial = SequencerState(StepList.empty, None, None, None, None)
}
