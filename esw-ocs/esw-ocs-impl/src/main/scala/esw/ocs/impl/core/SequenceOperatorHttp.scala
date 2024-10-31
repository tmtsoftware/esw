package esw.ocs.impl.core

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout
import esw.constants.SequencerTimeouts
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{MaybeNextResult, OkOrUnhandledResponse, PullNextResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
 * This class is to help execution of stepList(Sequence)
 *
 * @param seqF - Future reference to optional API (used to avoid recursive timing dependency with script server)
 * @param system - An ActorSystem
 */
private[ocs] class SequenceOperatorHttp(seqF: Future[Option[SequencerApi]])(implicit
    system: ActorSystem[?],
    ec: ExecutionContext
) extends SequenceOperatorApi {

  /**
   * This method sends a PullNext message to sequencer if successful it returns the pending step which is to be executed next
   *
   * @return a [[esw.ocs.api.protocol.PullNextResponse]] as Future value
   */
  def pullNext: Future[PullNextResponse] = seqF.flatMap(_.get.pullNext)

  /**
   * This method returns the next step Pending step if sequencer is in Running state otherwise returns None
   *
   * @return an Option of [[esw.ocs.api.models.Step]] as Future value
   */
  def maybeNext: Future[Option[Step]] = {
    seqF.flatMap(_.get.maybeNext).map {
      case MaybeNextResult(maybeStep) => maybeStep
      case _                          => None
    }
  }

  /**
   * This method is to determine whether next step is ready to execute or not. It returns Ok if the next step is ready for execution
   * otherwise it waits until it is ready to execute and then returns the Ok response.
   * Unhandled is returned when the ReadyToExecuteNext message is not acceptable by sequencer
   *
   * @return a [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as Future value
   */
  def readyToExecuteNext: Future[OkOrUnhandledResponse] = seqF.flatMap(_.get.readyToExecuteNext)

  /**
   * This method changes the status from InFlight to [[esw.ocs.api.models.StepStatus.Finished.Success]](Finished) for current running step
   */
  def stepSuccess(): Unit =
    seqF.flatMap(x => Future.successful(x.get.stepSuccess()))

  /**
   * This method changes the status from InFlight to [[esw.ocs.api.models.StepStatus.Finished.Failure]](Finished) for current running step
   */
  def stepFailure(message: String): Unit =
    seqF.flatMap(x => Future.successful(x.get.stepFailure(message)))
}
