package esw.ocs.framework.api.models.messages

sealed trait ProcessSequenceError

object ProcessSequenceError {
  case object DuplicateIdsFound           extends ProcessSequenceError
  case object ExistingSequenceIsInProcess extends ProcessSequenceError
}
