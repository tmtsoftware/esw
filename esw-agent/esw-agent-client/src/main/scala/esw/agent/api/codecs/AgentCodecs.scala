package esw.agent.api.codecs

import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem}
import csw.location.api.codec.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.api.{AgentCommand, ComponentStatus, Response}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

// $COVERAGE-OFF$
trait AgentCodecs extends CommonCodecs with LocationCodecs {
  implicit def actorSystem: ActorSystem[_]

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] = {
    val resolver = ActorRefResolver(actorSystem)

    Codec.bimap[String, ActorRef[T]](
      resolver.toSerializationFormat,
      resolver.resolveActorRef
    )
  }

  implicit lazy val agentCommandCodec: Codec[AgentCommand]       = deriveAllCodecs
  implicit lazy val componentStatusCodec: Codec[ComponentStatus] = deriveAllCodecs
  implicit lazy val agentResponseCodec: Codec[Response]          = deriveAllCodecs
}
// $COVERAGE-ON$
