package esw.ocs.api.models.request

sealed trait SequencerAdminPostRequest

object SequencerAdminPostRequest {
  case object GetSequence extends SequencerAdminPostRequest
}
