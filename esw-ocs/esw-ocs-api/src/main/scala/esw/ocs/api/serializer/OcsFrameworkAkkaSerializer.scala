package esw.ocs.api.serializer

import akka.Done
import akka.actor.typed.ActorSystem
import akka.serialization.Serializer
import csw.location.model.scaladsl.AkkaLocation
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.SequenceComponentMsg
import esw.ocs.api.models.messages.SequencerMsg.ExternalSequencerMsg
import esw.ocs.api.models.messages.error._
import io.bullet.borer.Cbor

class OcsFrameworkAkkaSerializer(_actorSystem: ActorSystem[_]) extends OcsFrameworkCodecs with Serializer {
  override implicit def actorSystem: ActorSystem[_] = _actorSystem

  override def identifier: Int = 29926
  private val logger: Logger   = new LoggerFactory("Sequencer-codec").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: ExternalSequencerMsg       => Cbor.encode(x).toByteArray
    case x: StepList                   => Cbor.encode(x).toByteArray
    case x: SequenceComponentMsg       => Cbor.encode(x).toByteArray
    case Right(x: AkkaLocation)        => Cbor.encode(x).toByteArray
    case Left(x: LoadScriptError)      => Cbor.encode(x).toByteArray
    case Left(x: EditorError)          => Cbor.encode(x).toByteArray
    case Left(x: ProcessSequenceError) => Cbor.encode(x).toByteArray
    case Some(x: StepList)             => Cbor.encode(x).toByteArray
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
    } else if (classOf[Either[LoadScriptError, AkkaLocation]].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Either[LoadScriptError, AkkaLocation]].value
    } else if (classOf[Either[ProcessSequenceError, SubmitResponse]].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Either[ProcessSequenceError, SubmitResponse]].value
    } else if (classOf[Either[EditorError, Done]].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Either[EditorError, Done]].value
    } else if (classOf[Option[StepList]].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Option[StepList]].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }

}
