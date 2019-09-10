package esw.ocs.api.responses

import esw.ocs.api.codecs.OcsAkkaSerializable

final case class RegistrationError(msg: String) extends OcsAkkaSerializable
