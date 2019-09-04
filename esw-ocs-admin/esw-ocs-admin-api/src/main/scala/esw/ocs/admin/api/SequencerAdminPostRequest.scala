package esw.ocs.admin.api

sealed trait SequencerAdminPostRequest

object SequencerAdminPostRequest {
  case class GetSequence(sequencerName: String) extends SequencerAdminPostRequest
}
