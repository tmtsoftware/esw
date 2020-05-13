package esw.sm.api.models

import esw.ocs.api.protocol.{LoadScriptError => OcsLoadScriptError}

sealed trait SequencerError extends Throwable {
  def msg: String
}

sealed trait AgentError extends SequencerError

object SequenceManagerError {
  case class LocationServiceError(msg: String)         extends AgentError
  case class SpawnSequenceComponentFailed(msg: String) extends AgentError

  case class LoadScriptError(error: OcsLoadScriptError, msg: String = s"Error while loading script") extends SequencerError

  def toSmLoadScriptError(error: OcsLoadScriptError): LoadScriptError =
    LoadScriptError(error, s"Error while loading script ${error}")

  case class SequencerNotIdle(obsMode: String) extends SequencerError {
    override def msg: String = s"Sequencers for $obsMode are already executing another sequence"
  }
}
