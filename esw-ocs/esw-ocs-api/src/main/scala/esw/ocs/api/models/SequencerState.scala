package esw.ocs.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class SequencerState extends EnumEntry with Lowercase
object SequencerState extends Enum[SequencerState] {

  def values: IndexedSeq[SequencerState] = findValues

  case object Idle             extends SequencerState
  case object Loaded           extends SequencerState
  case object InProgress       extends SequencerState
  case object Online           extends SequencerState
  case object Offline          extends SequencerState
  case object GoingOnline      extends SequencerState
  case object GoingOffline     extends SequencerState
  case object ShuttingDown     extends SequencerState
  case object AbortingSequence extends SequencerState
}
