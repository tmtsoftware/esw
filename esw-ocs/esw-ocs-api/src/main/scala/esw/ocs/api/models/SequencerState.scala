package esw.ocs.api.models

import akka.Done

import scala.concurrent.Promise
import scala.util.{Failure, Success}

//todo: make steplist option
case class SequencerState(
    stepList: StepList,
    previousStepList: Option[StepList],
    readyToExecuteNextPromise: Option[Promise[Done]],
    stepRefPromise: Option[Promise[Step]]
) {
  def completeReadyToExecuteNextPromise(): Unit =
    if (!stepList.isFinished)
      readyToExecuteNextPromise.foreach(_.complete(Success(Done)))

  def failStepRefPromise(): SequencerState = {
    stepRefPromise.foreach(_.complete(Failure(new RuntimeException("Trying to pull Step from a finished Sequence."))))
    copy(stepRefPromise = None)
  }

}

object SequencerState {
  def initial = SequencerState(StepList.empty, None, None, None)
}
