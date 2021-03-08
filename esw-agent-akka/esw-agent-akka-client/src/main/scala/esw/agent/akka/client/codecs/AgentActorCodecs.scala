package esw.agent.akka.client.codecs

import akka.actor.typed.ActorRef
import csw.location.api.codec.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.akka.client.AgentRemoteCommand
import esw.agent.akka.client.models._
import esw.agent.service.api.codecs.AgentCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}

trait AgentActorCodecs extends AgentCodecs with CommonCodecs with LocationCodecs {
  implicit def actorRefCodec[T]: Codec[ActorRef[T]]              = io.bullet.borer.compat.akka.typedActorRefCodec
  implicit lazy val agentCommandCodec: Codec[AgentRemoteCommand] = deriveAllCodecs
  implicit lazy val hostConfigCodec: Codec[HostConfig]           = deriveCodec
  implicit lazy val containerConfigCodec: Codec[ContainerConfig] = deriveCodec
  implicit lazy val containerInfoCodec: Codec[ContainerInfo]     = deriveCodec
  implicit lazy val componentInfoCodec: Codec[ComponentInfo]     = deriveCodec
}
