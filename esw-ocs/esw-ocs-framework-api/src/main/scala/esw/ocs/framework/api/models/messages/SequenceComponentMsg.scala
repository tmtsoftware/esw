package esw.ocs.framework.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.model.scaladsl.AkkaLocation
import esw.ocs.framework.api.models.serializer.OcsFrameworkSerializable

sealed trait SequenceComponentMsg extends OcsFrameworkSerializable

object SequenceComponentMsg {
  final case class LoadScript(
      sequencerId: String,
      observingMode: String,
      replyTo: ActorRef[Either[LoadScriptError, AkkaLocation]]
  ) extends SequenceComponentMsg
  final case class UnloadScript(replyTo: ActorRef[Done])              extends SequenceComponentMsg
  final case class GetStatus(replyTo: ActorRef[Option[AkkaLocation]]) extends SequenceComponentMsg
}
