package esw.ocs.api.models.messages.FSM

sealed trait SequencerResponse
sealed trait GoOnlineResponse extends SequencerResponse
sealed trait ShutdownResponse extends SequencerResponse

case class Unhandled(state: String, messageType: String) extends GoOnlineResponse with ShutdownResponse {
  val description = s"Sequencer can not handle '$messageType' message in '$state' state"
}

case object EswSuccess                                              extends GoOnlineResponse with ShutdownResponse
case class EswError(error: String, cause: Option[Throwable] = None) extends GoOnlineResponse with ShutdownResponse
object EswError {
  def apply(cause: Throwable) = EswError(cause.getMessage, Some(cause))
}
