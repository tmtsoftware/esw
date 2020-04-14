package esw.sm.messages

import akka.Done
import akka.actor.typed.ActorRef
import csw.location.api.models.HttpLocation

trait SequenceManagerMsg

object SequenceManagerMsg {
  case class Configure(obsMode: String, replyTo: ActorRef[ConfigureResponse]) extends SequenceManagerMsg
  case class Cleanup(obsMode: String, replyTo: ActorRef[Done])                extends SequenceManagerMsg
  case class GetRunningObsModes(replyTo: ActorRef[Set[String]])               extends SequenceManagerMsg

  private[sm] case class ConfigurationCompleted(res: ConfigureResponse) extends SequenceManagerMsg
}

sealed trait ConfigureResponse

object ConfigureResponse {
  case class Success(sequencerLocation: HttpLocation) extends ConfigureResponse

  sealed trait Failure                                      extends ConfigureResponse
  case object ConflictingResourcesWithRunningObsMode        extends Failure // todo : add conflicting obs mode
  case class FailedToStartSequencers(reasons: List[String]) extends Failure
  case class ConfigurationFailure(msg: String)              extends Failure
}
