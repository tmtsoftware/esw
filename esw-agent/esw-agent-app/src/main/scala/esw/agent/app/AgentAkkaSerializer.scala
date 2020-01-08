package esw.agent.app

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.Serializer
import esw.agent.api.codecs.AgentCodecs
import esw.agent.api.{AgentCommand, Response}
import io.bullet.borer.{Cbor, Decoder}

import scala.reflect.ClassTag

// $COVERAGE-OFF$
class AgentAkkaSerializer(_actorSystem: ExtendedActorSystem) extends AgentCodecs with Serializer {

  override def identifier: Int = 26726

  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: AgentCommand => Cbor.encode(x).toByteArray
    case x: Response     => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    def fromBinary[T: ClassTag: Decoder]: Option[T] = {
      val clazz = scala.reflect.classTag[T].runtimeClass
      if (clazz.isAssignableFrom(manifest.get)) Some(Cbor.decode(bytes).to[T].value)
      else None
    }
    {
      fromBinary[AgentCommand] orElse
      fromBinary[Response]
    }.getOrElse {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      throw ex
    }
  }
}
// $COVERAGE-ON$
