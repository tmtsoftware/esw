package esw.ocs.framework.api.models

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.api.models.AkkaLocation
import csw.serializable.TMTSerializable

sealed trait SequenceComponentMsg extends TMTSerializable

object SequenceComponentMsg {

  // todo: sender type = Either[LoadingFailed, AkkaLocation] ?
  //  LoadingFailed("Existing Script is already loaded with loc: $akkaLocation")
  case class LoadScript(sequencerId: String, observingMode: String, sender: ActorRef[Either[AkkaLocation, AkkaLocation]])
      extends SequenceComponentMsg
  case class StopScript(sender: ActorRef[Done])                extends SequenceComponentMsg
  case class GetStatus(sender: ActorRef[Option[AkkaLocation]]) extends SequenceComponentMsg
}
