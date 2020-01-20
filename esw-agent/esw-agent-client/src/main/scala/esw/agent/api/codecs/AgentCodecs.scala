package esw.agent.api.codecs

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, SerializationExtension}
import csw.location.models.codecs.LocationCodecs
import csw.prefix.codecs.CommonCodecs
import esw.agent.api.{AgentCommand, Response}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

// $COVERAGE-OFF$
trait AgentCodecs extends CommonCodecs with LocationCodecs {
  implicit def actorSystem: ActorSystem[_]

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] =
    Codec.bimap[String, ActorRef[T]](
      actorRef => Serialization.serializedActorPath(actorRef.toClassic),
      path => {
        val provider = SerializationExtension(actorSystem.toClassic).system.provider
        provider.resolveActorRef(path)
      }
    )

  implicit lazy val agentCommandCodec: Codec[AgentCommand] = deriveAllCodecs
  implicit lazy val agentResponseCodec: Codec[Response]    = deriveAllCodecs
}
// $COVERAGE-ON$
