package esw.agent.api.codecs

import csw.commons.codecs.ActorCodecs
import csw.location.api.codec.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.api.AgentRemoteCommand
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait AgentActorCodecs extends AgentCodecs with CommonCodecs with LocationCodecs with ActorCodecs {
  implicit lazy val agentCommandCodec: Codec[AgentRemoteCommand] = deriveAllCodecs
}
