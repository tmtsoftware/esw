package esw.ocs.impl.serializer

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.serialization.Serializer
import csw.command.client.messages.sequencer.SequencerMsg
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.ocs.api.models.codecs.OcsCodecs
import esw.ocs.api.models.StepList
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.api.models.responses._
import esw.ocs.impl.messages.{SequenceComponentMsg, SequencerState}
import esw.ocs.impl.messages.codecs.OcsMsgCodecs
import io.bullet.borer.{Cbor, Decoder}

import scala.reflect.ClassTag

class OcsAkkaSerializer(_actorSystem: ExtendedActorSystem) extends OcsCodecs with OcsMsgCodecs with Serializer {
  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29926
  private val logger: Logger   = new LoggerFactory("Sequencer-codec").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: EswSequencerMessage          => Cbor.encode(x).toByteArray
    case x: EswSequencerResponse         => Cbor.encode(x).toByteArray
    case x: StepList                     => Cbor.encode(x).toByteArray
    case x: SequencerState[SequencerMsg] => Cbor.encode(x).toByteArray
    case x: SequenceComponentMsg         => Cbor.encode(x).toByteArray
    case x: SequenceComponentResponse    => Cbor.encode(x).toByteArray
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
      fromBinary[EswSequencerMessage] orElse
      fromBinary[EswSequencerResponse] orElse
      fromBinary[SequencerState[SequencerMsg]] orElse
      fromBinary[StepList] orElse
      fromBinary[SequenceComponentMsg] orElse
      fromBinary[SequenceComponentResponse]
    }.getOrElse {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }

}
