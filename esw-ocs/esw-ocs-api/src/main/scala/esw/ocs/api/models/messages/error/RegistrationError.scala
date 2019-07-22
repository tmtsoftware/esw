package esw.ocs.api.models.messages.error

import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class RegistrationError(msg: String) extends OcsFrameworkAkkaSerializable
