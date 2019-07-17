package esw.ocs.framework.api.models.serializer

import akka.actor.typed.ActorSystem
import akka.serialization.Serializer
import csw.location.model.scaladsl.AkkaLocation
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.ocs.framework.api.models.codecs.SequenceComponentCodecs
import esw.ocs.framework.api.models.messages.SequenceComponentMsg
import io.bullet.borer.Cbor

class SequenceComponentSerializer(_actorSystem: ActorSystem[_]) extends Serializer with SequenceComponentCodecs {
  override def identifier: Int = 29925

  override implicit def actorSystem: ActorSystem[_] = _actorSystem

  private val logger: Logger = new LoggerFactory("SequenceComp").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    //fixme: Do we need for Option??
    case x: SequenceComponentMsg => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    if (classOf[SequenceComponentMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[SequenceComponentMsg].value
    } else if (classOf[Option[AkkaLocation]].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Option[AkkaLocation]].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
