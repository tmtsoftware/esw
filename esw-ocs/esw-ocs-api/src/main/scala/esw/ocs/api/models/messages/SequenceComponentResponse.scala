package esw.ocs.api.models.messages

import csw.location.models.AkkaLocation
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait SequenceComponentResponse extends OcsFrameworkAkkaSerializable with Product with Serializable

object SequenceComponentResponse {
  final case class LoadScriptResponse(response: Either[RegistrationError, AkkaLocation]) extends SequenceComponentResponse
  final case class GetStatusResponse(response: Option[AkkaLocation])                     extends SequenceComponentResponse
}
