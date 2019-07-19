package esw.ocs.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.model.scaladsl.AkkaLocation
import esw.ocs.api.models.messages.error.LoadScriptError
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait SequenceComponentMsg extends OcsFrameworkAkkaSerializable

object SequenceComponentMsg {
  final case class LoadScript(
      sequencerId: String,
      observingMode: String,
      replyTo: ActorRef[Either[LoadScriptError, AkkaLocation]]
  ) extends SequenceComponentMsg
  final case class UnloadScript(replyTo: ActorRef[Done])              extends SequenceComponentMsg
  final case class GetStatus(replyTo: ActorRef[Option[AkkaLocation]]) extends SequenceComponentMsg
}
