package esw.ocs.api.models.responses

import csw.location.models.AkkaLocation
import esw.ocs.api.codecs.OcsAkkaSerializable

sealed trait SequenceComponentResponse extends OcsAkkaSerializable

object SequenceComponentResponse {
  final case class LoadScriptResponse(response: Either[RegistrationError, AkkaLocation]) extends SequenceComponentResponse
  final case class GetStatusResponse(response: Option[AkkaLocation])                     extends SequenceComponentResponse
  case object Done                                                                       extends SequenceComponentResponse
}
