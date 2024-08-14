package esw.ocs.impl.core

import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.util.Timeout
import esw.constants.SequencerTimeouts
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.messages.SequencerMessages.*
import esw.ocs.api.client.SequencerClient
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{OkOrUnhandledResponse, PullNextResponse}

import scala.concurrent.Future

// XXX TODO FIXME: Could be merged with SequenceOperator if SequencerApi is used as arg

/**
 * This class is to help execution of stepList(Sequence)
 *
 * @param sequencer - Typed actor ref of the sequencer
 * @param system - An ActorSystem
 */
private[ocs] class SequenceOperatorHttp(sequencer: SequencerApi)(implicit system: ActorSystem[?]) extends SequenceOperatorApi {

  private implicit val timeout: Timeout = SequencerTimeouts.LongTimeout

  /**
   * This method sends a PullNext message to sequencer if successful it returns the pending step which is to be executed next
   *
   * @return a [[esw.ocs.api.protocol.PullNextResponse]] as Future value
   */
  def pullNext: Future[PullNextResponse] = sequencer.pullNext

  /**
   * This method returns the next step Pending step if sequencer is in Running state otherwise returns None
   *
   * @return an Option of [[esw.ocs.api.models.Step]] as Future value
   */
  def maybeNext: Future[Option[Step]] = sequencer.maybeNext

  /**
   * This method is to determine whether next step is ready to execute or not. It returns Ok if the next step is ready for execution
   * otherwise it waits until it is ready to execute and then returns the Ok response.
   * Unhandled is returned when the ReadyToExecuteNext message is not acceptable by sequencer
   *
   * @return a [[esw.ocs.api.protocol.OkOrUnhandledResponse]] as Future value
   */
  def readyToExecuteNext: Future[OkOrUnhandledResponse] = sequencer.readyToExecuteNext

  /**
   * This method changes the status from InFlight to [[esw.ocs.api.models.StepStatus.Finished.Success]](Finished) for current running step
   */
  def stepSuccess(): Unit = sequencer.stepSuccess()

  /**
   * This method changes the status from InFlight to [[esw.ocs.api.models.StepStatus.Finished.Failure]](Finished) for current running step
   */
  def stepFailure(message: String): Unit = sequencer.stepFailure(message)
}
