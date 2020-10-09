package esw.agent.akka.client.codecs

import akka.actor.typed.ActorRef
import csw.location.api.codec.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.akka.client.AgentRemoteCommand
import esw.agent.service.api.codecs.AgentCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait AgentActorCodecs extends AgentCodecs with CommonCodecs with LocationCodecs {
  implicit def actorRefCodec[T]: Codec[ActorRef[T]]              = io.bullet.borer.compat.akka.typedActorRefCodec
  implicit lazy val agentCommandCodec: Codec[AgentRemoteCommand] = deriveAllCodecs
}
