package esw.ocs.api.models.messages.FSM

sealed trait SequencerResponse
sealed trait GoOnlineResponse     extends SequencerResponse
sealed trait GoOfflineResponse    extends SequencerResponse
sealed trait ShutdownResponse     extends SequencerResponse
sealed trait LoadSequenceResponse extends SequencerResponse

case class Unhandled(state: String, messageType: String)
    extends GoOnlineResponse
    with ShutdownResponse
    with GoOfflineResponse
    with LoadSequenceResponse {
  val description = s"Sequencer can not handle '$messageType' message in '$state' state"
}

case object EswSuccess extends GoOnlineResponse with ShutdownResponse with GoOfflineResponse with LoadSequenceResponse

case class EswError(error: String, cause: Option[Throwable] = None)
    extends GoOnlineResponse
    with ShutdownResponse
    with GoOfflineResponse
    with LoadSequenceResponse

object EswError {
  def apply(cause: Throwable): EswError = EswError(cause.getMessage, Some(cause))
}
