package esw.sm.api.protocol

sealed trait SequenceManagerPostRequest
object SequenceManagerPostRequest {
  case object GetRunningObsModes extends SequenceManagerPostRequest
}
