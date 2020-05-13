package esw.ocs.api.actor.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.prefix.models.Subsystem
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptResponse, RestartScriptResponse}

sealed trait SequenceComponentMsg extends OcsAkkaSerializable

object SequenceComponentMsg {
  final case class LoadScript(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[LoadScriptResponse])
      extends SequenceComponentMsg
  final case class UnloadScript(replyTo: ActorRef[Done])             extends SequenceComponentMsg
  final case class Restart(replyTo: ActorRef[RestartScriptResponse]) extends SequenceComponentMsg

  final case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends SequenceComponentMsg
  private[ocs] final case object Stop                              extends SequenceComponentMsg
}
