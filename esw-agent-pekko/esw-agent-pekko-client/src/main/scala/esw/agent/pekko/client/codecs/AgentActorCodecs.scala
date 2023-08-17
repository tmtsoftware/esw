package esw.agent.pekko.client.codecs

import org.apache.pekko.actor.typed.ActorRef
import csw.location.api.codec.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.pekko.client.AgentRemoteCommand
import esw.agent.pekko.client.models.*
import esw.agent.service.api.codecs.AgentCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}

/**
 * Codecs for the models which are being used while communication by the actor
 */
trait AgentActorCodecs extends AgentCodecs with CommonCodecs with LocationCodecs {
  implicit def actorRefCodec[T]: Codec[ActorRef[T]]              = io.bullet.borer.compat.pekko.typedActorRefCodec
  implicit lazy val agentCommandCodec: Codec[AgentRemoteCommand] = deriveAllCodecs
  implicit lazy val hostConfigCodec: Codec[HostConfig]           = deriveCodec
  implicit lazy val containerConfigCodec: Codec[ContainerConfig] = deriveCodec
  implicit lazy val containerInfoCodec: Codec[ContainerInfo]     = deriveCodec
  implicit lazy val componentInfoCodec: Codec[ComponentInfo]     = deriveCodec
}
