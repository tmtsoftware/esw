package esw.ocs.framework.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.model.scaladsl.AkkaLocation
import esw.ocs.framework.api.models.serializer.SequenceComponentSerializable

sealed trait SequenceComponentMsg extends SequenceComponentSerializable

object SequenceComponentMsg {
  case class LoadScript(
      sequencerId: String,
      observingMode: String,
      sender: ActorRef[Option[AkkaLocation]]
  ) extends SequenceComponentMsg
  case class UnloadScript(sender: ActorRef[Done])              extends SequenceComponentMsg
  case class GetStatus(sender: ActorRef[Option[AkkaLocation]]) extends SequenceComponentMsg
}
