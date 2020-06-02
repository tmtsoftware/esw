package esw.ocs.api.protocol

import csw.location.api.models.AkkaLocation
import csw.prefix.models.Prefix
import esw.ocs.api.codecs.OcsAkkaSerializable

final case class ScriptResponse(response: Either[ScriptError, AkkaLocation]) extends OcsAkkaSerializable
final case class GetStatusResponse(response: Option[AkkaLocation])           extends OcsAkkaSerializable

sealed trait ScriptError extends Throwable {
  def msg: String
}

object ScriptError {
  case class SequenceComponentNotIdle(runningSequencer: Prefix) extends ScriptError {
    override def msg: String =
      s"Error: Sequence component is not idle. Already loaded sequencer script for: $runningSequencer"
  }
  case class LocationServiceError(msg: String) extends ScriptError
  case class LoadingScriptFailed(msg: String)  extends ScriptError
  case object RestartNotSupportedInIdle extends ScriptError {
    override def msg: String =
      "Error: Sequence Component is not loaded with sequencer script. Restart sequencer is not supported operation"
  }
}
