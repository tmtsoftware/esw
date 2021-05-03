package esw.ocs.api.models

sealed trait SequencerState

object SequencerState {
  case object Idle       extends SequencerState
  case object Loaded     extends SequencerState
  case object Running    extends SequencerState
  case object Offline    extends SequencerState
  case object Processing extends SequencerState
}
