package esw.ocs.api.models.messages

import csw.location.models.AkkaLocation
import esw.ocs.api.models.messages.error.RegistrationError
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait SequenceComponentResponse extends OcsFrameworkAkkaSerializable

object SequenceComponentResponse {
  case class LoadScriptResponse(response: Either[RegistrationError, AkkaLocation]) extends SequenceComponentResponse
  case class GetStatusResponse(response: Option[AkkaLocation])                     extends SequenceComponentResponse
}
