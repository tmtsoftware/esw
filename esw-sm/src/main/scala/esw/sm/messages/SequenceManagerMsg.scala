package esw.sm.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.api.models.HttpLocation
import csw.prefix.models.Prefix
import esw.sm.models.ObservingMode

trait SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(observingMode: ObservingMode, replyTo: ActorRef[ConfigureResponse]) extends SequenceManagerMsg
  case class Cleanup(observingMode: ObservingMode, replyTo: ActorRef[Done])                extends SequenceManagerMsg
  case class GetRunningObsModes(replyTo: ActorRef[Set[ObservingMode]])                     extends SequenceManagerMsg
}

sealed trait ConfigureResponse

object ConfigureResponse {
  case class Success(location: HttpLocation) extends ConfigureResponse

  sealed trait Failure                               extends ConfigureResponse
  case object AlreadyRunningObservingMode            extends Failure
  case object ConflictingResourcesWithRunningObsMode extends Failure
  case class ConfigurationFailure(msg: String)       extends Failure

}
