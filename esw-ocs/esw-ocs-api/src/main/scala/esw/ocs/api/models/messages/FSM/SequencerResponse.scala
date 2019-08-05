package esw.ocs.api.models.messages.FSM

sealed trait SequencerResponse

case class Unhandled(state: String, messageType: String) extends SequencerResponse {
  val description = s"Sequencer can not accept '$messageType' message in '$state' state"
}

case object SequencerSuccess extends SequencerResponse

case class SequencerError[T](error: String, cause: Option[T] = None) extends SequencerResponse

object SequencerError {
  def apply[T](cause: T): SequencerError[T] = cause match {
    case err: Throwable => SequencerError(err.getMessage, Some(cause))
    case x              => SequencerError("error", Some(x))
  }
}

case class SequencerResult[T](value: T) extends SequencerResponse
