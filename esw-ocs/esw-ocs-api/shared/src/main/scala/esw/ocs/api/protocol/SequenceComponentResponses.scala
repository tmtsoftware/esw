package esw.ocs.api.protocol

import csw.location.api.models.AkkaLocation
import esw.ocs.api.codecs.OcsAkkaSerializable

final case class LoadScriptResponse(response: Either[LoadScriptError, AkkaLocation])       extends OcsAkkaSerializable
final case class RestartScriptResponse(response: Either[RestartScriptError, AkkaLocation]) extends OcsAkkaSerializable
final case class GetStatusResponse(response: Option[AkkaLocation])                         extends OcsAkkaSerializable

sealed trait StartSequencerError extends RestartScriptError with LoadScriptError {
  def msg: String
}
sealed trait RestartScriptError
sealed trait LoadScriptError

object ScriptError {
  case class LocationServiceError(msg: String) extends StartSequencerError
  case class ScriptError(msg: String)          extends StartSequencerError // fixme: add error type for script config missing

  case object RestartNotAllowedInIdleState extends RestartScriptError

  case object SequenceComponentNotIdle extends LoadScriptError
}
