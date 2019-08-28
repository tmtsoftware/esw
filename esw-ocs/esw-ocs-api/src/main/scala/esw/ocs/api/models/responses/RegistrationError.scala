package esw.ocs.api.models.responses

import esw.ocs.api.models.codecs.OcsAkkaSerializable

final case class RegistrationError(msg: String) extends OcsAkkaSerializable
