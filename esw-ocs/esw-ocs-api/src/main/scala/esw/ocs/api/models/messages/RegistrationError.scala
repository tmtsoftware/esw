package esw.ocs.api.models.messages

import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class RegistrationError(msg: String) extends OcsFrameworkAkkaSerializable
