package esw.ocs.framework.api.models.messages.error

import esw.ocs.framework.api.models.serializer.OcsFrameworkAkkaSerializable

sealed trait ProcessSequenceError extends OcsFrameworkAkkaSerializable

object ProcessSequenceError {
  case object DuplicateIdsFound           extends ProcessSequenceError
  case object ExistingSequenceIsInProcess extends ProcessSequenceError
}
