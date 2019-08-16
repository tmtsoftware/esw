package esw.ocs.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import esw.ocs.api.models.messages.SequenceComponentResponse.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.serializer.OcsAkkaSerializable

sealed trait SequenceComponentMsg extends OcsAkkaSerializable with Product with Serializable

object SequenceComponentMsg {
  final case class LoadScript(
      sequencerId: String,
      observingMode: String,
      replyTo: ActorRef[LoadScriptResponse]
  ) extends SequenceComponentMsg
  final case class UnloadScript(replyTo: ActorRef[Done])           extends SequenceComponentMsg
  final case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends SequenceComponentMsg
  private[ocs] final case object Stop                              extends SequenceComponentMsg
}
