package esw.ocs.framework.api.models.messages

import esw.ocs.framework.api.models.serializer.SequencerSerializable

sealed trait ProcessSequenceError extends SequencerSerializable

object ProcessSequenceError {
  case object DuplicateIdsFound           extends ProcessSequenceError
  case object ExistingSequenceIsInProcess extends ProcessSequenceError
}
