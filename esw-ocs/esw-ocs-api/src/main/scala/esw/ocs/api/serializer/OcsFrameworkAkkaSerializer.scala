package esw.ocs.api.serializer

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.SequencerMessages.{ExternalEditorSequencerMsg, LifecycleMsg}
import esw.ocs.api.models.messages.{
  EditorResponse,
  LifecycleResponse,
  SequenceComponentMsg,
  SequenceComponentResponse,
  StepListResponse
}
import io.bullet.borer.Cbor

class OcsFrameworkAkkaSerializer(_actorSystem: ExtendedActorSystem) extends OcsFrameworkCodecs with Serializer {
  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29926
  private val logger: Logger   = new LoggerFactory("Sequencer-codec").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: LifecycleMsg               => Cbor.encode(x).toByteArray
    case x: ExternalEditorSequencerMsg => Cbor.encode(x).toByteArray
    case x: StepList                   => Cbor.encode(x).toByteArray
    case x: SequenceComponentMsg       => Cbor.encode(x).toByteArray
    case x: SequenceComponentResponse  => Cbor.encode(x).toByteArray
    case x: LifecycleResponse          => Cbor.encode(x).toByteArray
    case x: EditorResponse             => Cbor.encode(x).toByteArray
    case x: StepListResponse           => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    if (classOf[ExternalEditorSequencerMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[ExternalEditorSequencerMsg].value
    } else if (classOf[StepList].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[StepList].value
    } else if (classOf[LifecycleMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[LifecycleMsg].value
    } else if (classOf[LifecycleResponse].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[LifecycleResponse].value
    } else if (classOf[SequenceComponentMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[SequenceComponentMsg].value
    } else if (classOf[SequenceComponentResponse].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[SequenceComponentResponse].value
    } else if (classOf[EditorResponse].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[EditorResponse].value
    } else if (classOf[StepListResponse].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[StepListResponse].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }

}
