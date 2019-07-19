package esw.ocs.api.models.messages.error

import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait ProcessSequenceError extends OcsFrameworkAkkaSerializable

object ProcessSequenceError {
  case object DuplicateIdsFound           extends ProcessSequenceError
  case object ExistingSequenceIsInProcess extends ProcessSequenceError
}
