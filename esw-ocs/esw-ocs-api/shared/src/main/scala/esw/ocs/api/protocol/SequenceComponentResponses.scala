package esw.ocs.api.protocol

import csw.location.api.models.AkkaLocation
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.SequenceComponentState

object SequenceComponentResponse {

  sealed trait ScriptResponseOrUnhandled    extends OcsAkkaSerializable
  sealed trait GetStatusResponseOrUnhandled extends OcsAkkaSerializable
  sealed trait OkOrUnhandled                extends OcsAkkaSerializable

  final case object Ok extends OkOrUnhandled

  final case class Unhandled(state: SequenceComponentState, messageType: String, msg: String)
      extends ScriptResponseOrUnhandled
      with GetStatusResponseOrUnhandled
      with OkOrUnhandled
  object Unhandled {
    def apply(state: SequenceComponentState, messageType: String): Unhandled =
      new Unhandled(state, messageType, s"Sequence Component can not accept '$messageType' message in '${state.entryName}'")
  }

  final case class ScriptResponse(response: Either[ScriptError, AkkaLocation]) extends ScriptResponseOrUnhandled
  final case class GetStatusResponse(response: Option[AkkaLocation])           extends GetStatusResponseOrUnhandled
}

sealed trait ScriptError extends Throwable {
  def msg: String
}

object ScriptError {
  case class LocationServiceError(msg: String) extends ScriptError
  case class LoadingScriptFailed(msg: String)  extends ScriptError
}
