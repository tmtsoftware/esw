package esw.ocs.framework.api.models.serializer

import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.framework.api.models.codecs.SequencerCodecs
import esw.ocs.framework.api.models.messages.ProcessSequenceError
import esw.ocs.framework.api.models.messages.StepListError._
import esw.ocs.framework.api.models.{Sequence, Step, StepList}
import io.bullet.borer.Cbor

class SequencerSerializer extends SequencerCodecs with Serializer {
  override def identifier: Int = 29926

  private val logger: Logger = new LoggerFactory("Sequencer").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: Step                  => Cbor.encode(x).toByteArray
    case x: SubmitResponse        => Cbor.encode(x).toByteArray
    case x: Sequence              => Cbor.encode(x).toByteArray
    case x: ProcessSequenceError  => Cbor.encode(x).toByteArray
    case x: StepList              => Cbor.encode(x).toByteArray
    case x: ResetError            => Cbor.encode(x).toByteArray
    case x: ResumeError           => Cbor.encode(x).toByteArray
    case x: PauseError            => Cbor.encode(x).toByteArray
    case x: RemoveBreakpointError => Cbor.encode(x).toByteArray
    case x: AddBreakpointError    => Cbor.encode(x).toByteArray
    case x: DeleteError           => Cbor.encode(x).toByteArray
    case x: InsertError           => Cbor.encode(x).toByteArray
    case x: ReplaceError          => Cbor.encode(x).toByteArray
    case x: PrependError          => Cbor.encode(x).toByteArray
    case x: AddError              => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    if (classOf[Step].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Step].value
    } else if (classOf[SubmitResponse].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[SubmitResponse].value
    } else if (classOf[Sequence].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Sequence].value
    } else if (classOf[ProcessSequenceError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[ProcessSequenceError].value
    } else if (classOf[StepList].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[StepList].value
    } else if (classOf[ResetError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[ResetError].value
    } else if (classOf[ResumeError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[ResumeError].value
    } else if (classOf[PauseError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[PauseError].value
    } else if (classOf[RemoveBreakpointError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[RemoveBreakpointError].value
    } else if (classOf[AddBreakpointError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[AddBreakpointError].value
    } else if (classOf[DeleteError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[DeleteError].value
    } else if (classOf[InsertError].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[InsertError].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
