package esw.ocs.app

sealed trait SequencerAdminPostRequest

object SequencerAdminPostRequest {
  case class GetSequence() extends SequencerAdminPostRequest
}
