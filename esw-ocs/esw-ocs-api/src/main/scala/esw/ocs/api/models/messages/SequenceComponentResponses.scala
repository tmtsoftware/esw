package esw.ocs.api.models.messages

import csw.location.models.AkkaLocation
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

object SequenceComponentResponses {
  final case class LoadScriptResponse(response: Either[RegistrationError, AkkaLocation]) extends OcsFrameworkAkkaSerializable
  final case class GetStatusResponse(response: Option[AkkaLocation])                     extends OcsFrameworkAkkaSerializable
}
