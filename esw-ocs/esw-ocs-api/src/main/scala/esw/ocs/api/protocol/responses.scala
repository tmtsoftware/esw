package esw.ocs.api.protocol

import csw.location.models.AkkaLocation
import esw.ocs.api.codecs.OcsAkkaSerializable

final case class LoadScriptResponse(response: Either[LoadScriptError, AkkaLocation]) extends OcsAkkaSerializable
final case class GetStatusResponse(response: Option[AkkaLocation])                   extends OcsAkkaSerializable
final case class LoadScriptError(msg: String)
