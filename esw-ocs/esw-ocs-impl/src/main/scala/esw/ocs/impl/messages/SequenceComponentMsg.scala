package esw.ocs.impl.messages

import akka.actor.typed.ActorRef
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.responses.SequenceComponentResponse.{Done, GetStatusResponse, LoadScriptResponse}

sealed trait SequenceComponentMsg extends OcsAkkaSerializable

object SequenceComponentMsg {
  final case class LoadScript(
      sequencerId: String,
      observingMode: String,
      replyTo: ActorRef[LoadScriptResponse]
  ) extends SequenceComponentMsg
  final case class UnloadScript(replyTo: ActorRef[Done.type])      extends SequenceComponentMsg
  final case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends SequenceComponentMsg
  private[ocs] final case object Stop                              extends SequenceComponentMsg
}
