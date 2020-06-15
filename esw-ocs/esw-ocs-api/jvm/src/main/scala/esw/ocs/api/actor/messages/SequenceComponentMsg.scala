package esw.ocs.api.actor.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.prefix.models.Subsystem
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.protocol.{GetStatusResponse, ScriptResponse}

sealed trait SequenceComponentMsg
sealed trait SequenceComponentRemoteMsg extends SequenceComponentMsg with OcsAkkaSerializable

object SequenceComponentMsg {
  final case class LoadScript(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[ScriptResponse])
      extends SequenceComponentRemoteMsg
  final case class UnloadScript(replyTo: ActorRef[Done])      extends SequenceComponentRemoteMsg
  final case class Restart(replyTo: ActorRef[ScriptResponse]) extends SequenceComponentRemoteMsg

  final case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends SequenceComponentRemoteMsg
  final case class Shutdown(replyTo: ActorRef[Done])               extends SequenceComponentRemoteMsg

  private[ocs] final case class ShutdownInternal(replyTo: ActorRef[Done]) extends SequenceComponentMsg
  private[ocs] final case object Stop                                     extends SequenceComponentMsg
}
