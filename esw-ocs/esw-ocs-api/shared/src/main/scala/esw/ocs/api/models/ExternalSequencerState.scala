package esw.ocs.api.models

sealed trait ExternalSequencerState

object ExternalSequencerState {
  case object Idle       extends ExternalSequencerState
  case object Loaded     extends ExternalSequencerState
  case object Running    extends ExternalSequencerState
  case object Offline    extends ExternalSequencerState
  case object Processing extends ExternalSequencerState
}
