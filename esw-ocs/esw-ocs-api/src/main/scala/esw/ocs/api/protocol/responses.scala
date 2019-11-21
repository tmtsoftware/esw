package esw.ocs.api.protocol

import csw.location.models.AkkaLocation
import esw.ocs.api.codecs.OcsAkkaSerializable

final case class ScriptResponse(response: Either[ScriptError, AkkaLocation]) extends OcsAkkaSerializable
final case class GetStatusResponse(response: Option[AkkaLocation])           extends OcsAkkaSerializable
final case class ScriptError(msg: String)
