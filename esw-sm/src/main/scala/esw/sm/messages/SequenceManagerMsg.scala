package esw.sm.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.api.models.HttpLocation
import esw.ocs.api.protocol.ScriptError

trait SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(observingMode: String, replyTo: ActorRef[ConfigureResponse]) extends SequenceManagerMsg
  case class Cleanup(observingMode: String, replyTo: ActorRef[Done])                extends SequenceManagerMsg
  case class GetRunningObsModes(replyTo: ActorRef[Set[String]])                     extends SequenceManagerMsg
}

sealed trait ConfigureResponse

object ConfigureResponse {
  case class Success(sequencerLocation: HttpLocation) extends ConfigureResponse

  sealed trait Failure                                           extends ConfigureResponse
  case object ConflictingResourcesWithRunningObsMode             extends Failure
  case class ConfigurationFailure(msg: String)                   extends Failure
  case class FailedToStartSequencers(reasons: List[ScriptError]) extends Failure

}
