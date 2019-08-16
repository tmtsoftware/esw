package esw.ocs.api.models.messages

import esw.ocs.api.serializer.OcsAkkaSerializable

final case class RegistrationError(msg: String) extends OcsAkkaSerializable
