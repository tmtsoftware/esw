package esw.ocs.framework.api.models.serializer

import akka.actor.typed.ActorSystem
import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.ocs.framework.api.models.StepList
import esw.ocs.framework.api.models.codecs.OcsFrameworkCodecs
import esw.ocs.framework.api.models.messages.SequenceComponentMsg
import esw.ocs.framework.api.models.messages.SequencerMsg.ExternalSequencerMsg
import io.bullet.borer.Cbor

class OcsFrameworkSerializer(_actorSystem: ActorSystem[_]) extends OcsFrameworkCodecs with Serializer {
  override implicit def actorSystem: ActorSystem[_] = _actorSystem

  override def identifier: Int = 29926
  private val logger: Logger   = new LoggerFactory("Sequencer-codec").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: ExternalSequencerMsg => Cbor.encode(x).toByteArray
    case x: StepList             => Cbor.encode(x).toByteArray
    case x: SequenceComponentMsg => Cbor.encode(x).toByteArray
    //fixme: Do we need for Option??
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    if (classOf[ExternalSequencerMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[ExternalSequencerMsg].value
    } else if (classOf[StepList].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[StepList].value
    } else if (classOf[SequenceComponentMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[SequenceComponentMsg].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }

}
