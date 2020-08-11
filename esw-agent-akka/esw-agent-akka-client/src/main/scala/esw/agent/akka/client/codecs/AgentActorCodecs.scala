package esw.agent.akka.client.codecs

import csw.commons.codecs.ActorCodecs
import csw.location.api.codec.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.akka.client.AgentRemoteCommand
import esw.agent.service.api.codecs.AgentCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait AgentActorCodecs extends AgentCodecs with CommonCodecs with LocationCodecs with ActorCodecs {
  implicit lazy val agentCommandCodec: Codec[AgentRemoteCommand] = deriveAllCodecs
}
