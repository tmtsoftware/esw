package esw.ocs.api.protocol

import csw.location.api.models.PekkoLocation
import esw.ocs.api.codecs.OcsPekkoSerializable
import esw.ocs.api.models.SequenceComponentState
import esw.ocs.api.protocol.SequenceComponentResponse.ScriptErrorOrSequencerLocation

/*
 * These models are being used as responses for sequence components e.g., in SequenceComponentApi
 */
object SequenceComponentResponse {

  sealed trait ScriptResponseOrUnhandled extends OcsPekkoSerializable
  sealed trait OkOrUnhandled             extends OcsPekkoSerializable

  final case class GetStatusResponse(response: Option[PekkoLocation]) extends OcsPekkoSerializable

  sealed trait ScriptErrorOrSequencerLocation extends ScriptResponseOrUnhandled

  case object Ok extends OkOrUnhandled

  final case class Unhandled(state: SequenceComponentState, messageType: String, msg: String)
      extends ScriptResponseOrUnhandled
      with OkOrUnhandled

  object Unhandled {
    def apply(state: SequenceComponentState, messageType: String): Unhandled =
      new Unhandled(state, messageType, s"Sequence Component can not accept '$messageType' message in '${state.entryName}'")
  }

  final case class SequencerLocation(location: PekkoLocation) extends ScriptErrorOrSequencerLocation
}

sealed trait ScriptError extends Exception with ScriptErrorOrSequencerLocation {
  def msg: String
  override def getMessage: String = msg
}

object ScriptError {
  case class LocationServiceError(msg: String) extends ScriptError
  case class LoadingScriptFailed(msg: String)  extends ScriptError
}
