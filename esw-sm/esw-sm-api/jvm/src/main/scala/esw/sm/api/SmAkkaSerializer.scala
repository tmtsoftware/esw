package esw.sm.api

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.sm.api.actor.codecs.SequenceManagerMsgCodecs
import esw.sm.api.actor.messages.SequenceManagerRemoteMsg
import esw.sm.api.codecs.SequenceManagerCodecs
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}
import io.bullet.borer.{Cbor, Decoder}

import scala.reflect.ClassTag

class SmAkkaSerializer(_actorSystem: ExtendedActorSystem)
    extends SequenceManagerMsgCodecs
    with SequenceManagerCodecs
    with Serializer {
  implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29945
  private val logger: Logger   = new LoggerFactory(Prefix(ESW, "sequence_manager_codec")).getLogger

  override def toBinary(o: AnyRef): Array[Byte] =
    o match {
      case x: SequenceManagerRemoteMsg   => Cbor.encode(x).toByteArray
      case x: CleanupResponse            => Cbor.encode(x).toByteArray
      case x: GetRunningObsModesResponse => Cbor.encode(x).toByteArray
      case x: ConfigureResponse          => Cbor.encode(x).toByteArray
      case x: SequenceManagerState       => Cbor.encode(x).toByteArray
      case _ =>
        val ex = new RuntimeException(s"does not support encoding of $o")
        logger.error(ex.getMessage, ex = ex)
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
      fromBinary[SequenceManagerRemoteMsg] orElse
      fromBinary[CleanupResponse] orElse
      fromBinary[GetRunningObsModesResponse] orElse
      fromBinary[ConfigureResponse] orElse
      fromBinary[SequenceManagerState]
    }.getOrElse {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
