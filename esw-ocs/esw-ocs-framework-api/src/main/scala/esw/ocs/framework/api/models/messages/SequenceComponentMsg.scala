package esw.ocs.framework.api.models.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.api.models.AkkaLocation
import csw.serializable.TMTSerializable

import scala.util.Try

sealed trait SequenceComponentMsg extends TMTSerializable

object SequenceComponentMsg {
  case class LoadScript(
      sequencerId: String,
      observingMode: String,
      sender: ActorRef[Try[AkkaLocation]]
  ) extends SequenceComponentMsg
  case class StopScript(sender: ActorRef[Done])                extends SequenceComponentMsg
  case class GetStatus(sender: ActorRef[Option[AkkaLocation]]) extends SequenceComponentMsg
}
