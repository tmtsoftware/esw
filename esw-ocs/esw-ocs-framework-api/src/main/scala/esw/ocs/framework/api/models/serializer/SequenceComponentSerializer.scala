package esw.ocs.framework.api.models.serializer

import akka.serialization.Serializer
import csw.location.api.codec.DoneCodec
import csw.location.model.codecs.LocationCodecs
import csw.location.model.scaladsl.Location
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import io.bullet.borer.Cbor

class SequenceComponentSerializer extends Serializer with LocationCodecs with DoneCodec {
  override def identifier: Int = 29925

  private val logger: Logger = new LoggerFactory("SequenceComp").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: Location => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    if (classOf[Location].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Location].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
