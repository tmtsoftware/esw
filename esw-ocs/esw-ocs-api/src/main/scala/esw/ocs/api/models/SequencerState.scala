package esw.ocs.api.models

import akka.Done
import akka.actor.typed.ActorRef

//todo: make steplist option
case class SequencerState(
    stepList: StepList,
    previousStepList: Option[StepList],
    readyToExecuteSubscriber: Option[ActorRef[Done]],
    stepRefSubscriber: Option[ActorRef[Step]]
//    readyToExecuteNextPromise: Option[Promise[Done]],
//    stepRefPromise: Option[Promise[Step]]
) {
//  def completeReadyToExecuteNextPromise(): Unit =
//    if (!stepList.isFinished)
//      readyToExecuteNextPromise.foreach(_.complete(Success(Done)))
//
//  def failStepRefPromise(): SequencerState = {
//    stepRefPromise.foreach(_.complete(Failure(new RuntimeException("Trying to pull Step from a finished Sequence."))))
//    copy(stepRefPromise = None)
//  }

}

object SequencerState {
  def initial = SequencerState(StepList.empty, None) //, None, None)
}
