package esw.ocs.api

import csw.prefix.models.Subsystem
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok, ScriptResponseOrUnhandled}

import scala.concurrent.Future

/**
 * This API is for the sequence component. This model provides method based APIs for command interactions with a sequence component.
 */
trait SequenceComponentApi {

  /**
   * Starts the sequencer and Load the script with the given prefix
   * If it is successful then [[esw.ocs.api.protocol.SequenceComponentResponse.SequencerLocation]] is returned
   *
   * @param prefix - subsystem of sequencer
   * @return a [[esw.ocs.api.protocol.SequenceComponentResponse.ScriptResponseOrUnhandled]] as a Future value
   */
  def loadScript(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation] = None): Future[ScriptResponseOrUnhandled]

  /**
   * Restarts the sequencer. In other words, unloads the running script and loads it again.
   * If it is successful then [[esw.ocs.api.protocol.SequenceComponentResponse.SequencerLocation]] is returned
   *
   * @return a [[esw.ocs.api.protocol.SequenceComponentResponse.ScriptResponseOrUnhandled]] as a Future value
   */
  def restartScript(): Future[ScriptResponseOrUnhandled]

  /**
   * Shuts down the sequencer. In other words, unloads the running script.
   * If it is successful then [[esw.ocs.api.protocol.SequenceComponentResponse.Ok]] is returned
   *
   * @return an [[esw.ocs.api.protocol.SequenceComponentResponse.Ok]] as a Future value
   */
  def unloadScript(): Future[Ok.type]

  /**
   * Returns option of running(on this sequence component) sequencer's Location
   *
   * @return a [[esw.ocs.api.protocol.SequenceComponentResponse.GetStatusResponse]] as a Future value
   */
  def status: Future[GetStatusResponse]

  /**
   * Shuts down itself (sequence component).
   * If it is successful then [[esw.ocs.api.protocol.SequenceComponentResponse.Ok]] is returned
   *
   * @return an [[esw.ocs.api.protocol.SequenceComponentResponse.Ok]] as a Future value
   */
  def shutdown(): Future[Ok.type]
}
