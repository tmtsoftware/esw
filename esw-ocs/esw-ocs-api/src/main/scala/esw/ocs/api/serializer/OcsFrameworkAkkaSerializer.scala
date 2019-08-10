package esw.ocs.api.serializer

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.SequenceComponentResponses.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages._
import io.bullet.borer.{Cbor, Decoder}

import scala.reflect.ClassTag

class OcsFrameworkAkkaSerializer(_actorSystem: ExtendedActorSystem) extends OcsFrameworkCodecs with Serializer {
  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 29926
  private val logger: Logger   = new LoggerFactory("Sequencer-codec").getLogger

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: StartSequence        => Cbor.encode(x).toByteArray
    case x: LoadSequence         => Cbor.encode(x).toByteArray
    case x: LoadSequenceResponse => Cbor.encode(x).toByteArray
    case x: StepList             => Cbor.encode(x).toByteArray
    case x: SequenceComponentMsg => Cbor.encode(x).toByteArray
    case x: LoadScriptResponse   => Cbor.encode(x).toByteArray
    case x: GetStatusResponse    => Cbor.encode(x).toByteArray
    case x: GetSequenceResult    => Cbor.encode(x).toByteArray
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
      fromBinary[StepList] orElse
      fromBinary[SequenceComponentMsg] orElse
      fromBinary[LoadScriptResponse] orElse
      fromBinary[GetStatusResponse] orElse
      fromBinary[GetSequenceResult] orElse
      fromBinary[LoadSequence] orElse
      fromBinary[LoadSequenceResponse] orElse
      fromBinary[StartSequence]
    }.getOrElse {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }

}
