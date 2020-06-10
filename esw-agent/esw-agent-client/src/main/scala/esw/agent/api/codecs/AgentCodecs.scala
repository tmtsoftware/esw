package esw.agent.api.codecs

import csw.commons.codecs.ActorCodecs
import csw.location.api.codec.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.api.{AgentCommand, ComponentStatus, Response}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait AgentCodecs extends CommonCodecs with LocationCodecs with ActorCodecs {
  implicit lazy val agentCommandCodec: Codec[AgentCommand]       = deriveAllCodecs
  implicit lazy val componentStatusCodec: Codec[ComponentStatus] = deriveAllCodecs
  implicit lazy val agentResponseCodec: Codec[Response]          = deriveAllCodecs
}
