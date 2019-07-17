package esw.ocs.framework.api.models.messages

import esw.ocs.framework.api.models.serializer.OcsFrameworkSerializable

sealed trait ProcessSequenceError extends OcsFrameworkSerializable

object ProcessSequenceError {
  case object DuplicateIdsFound           extends ProcessSequenceError
  case object ExistingSequenceIsInProcess extends ProcessSequenceError
}
