package esw.ocs.api.models.messages.error

import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class LoadScriptError(msg: String) extends OcsFrameworkAkkaSerializable
