package esw.ocs.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class SequencerBehaviorState extends EnumEntry with Lowercase
object SequencerBehaviorState extends Enum[SequencerBehaviorState] {

  def values: IndexedSeq[SequencerBehaviorState] = findValues

  case object Idle         extends SequencerBehaviorState
  case object Loaded       extends SequencerBehaviorState
  case object InProgress   extends SequencerBehaviorState
  case object Online       extends SequencerBehaviorState
  case object Offline      extends SequencerBehaviorState
  case object GoingOnline  extends SequencerBehaviorState
  case object GoingOffline extends SequencerBehaviorState
  case object ShuttingDown extends SequencerBehaviorState
}
