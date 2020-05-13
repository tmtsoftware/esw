package esw.sm.api.models

import esw.ocs.api.protocol.{LoadScriptError => OcsLoadScriptError}

sealed trait SequencerError extends Throwable {
  def msg: String
}

sealed trait AgentError extends SequencerError

object SequenceManagerError {
  case class LocationServiceError(msg: String)         extends AgentError
  case class SpawnSequenceComponentFailed(msg: String) extends AgentError

  case class LoadScriptError(error: OcsLoadScriptError) extends SequencerError {
    override def msg: String = error.msg
  }

  def toSmLoadScriptError(error: OcsLoadScriptError): LoadScriptError =
    LoadScriptError(error)

}
