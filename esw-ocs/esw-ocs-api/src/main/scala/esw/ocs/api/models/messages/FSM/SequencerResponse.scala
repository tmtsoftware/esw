package esw.ocs.api.models.messages.FSM

sealed trait SequencerResponse
sealed trait SequencerResult extends SequencerResponse {
  type T
  val value: T
}

case class Unhandled(state: String, messageType: String) extends SequencerResponse {
  val description = s"Sequencer can not handle '$messageType' message in '$state' state"
}

case object EswSuccess extends SequencerResponse

case class EswError(error: String, cause: Option[Throwable] = None) extends SequencerResponse

object EswError {
  def apply(cause: Throwable): EswError = EswError(cause.getMessage, Some(cause))
}
