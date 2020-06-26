package esw.sm.api

import enumeratum.{Enum, EnumEntry}
import esw.sm.api.codecs.SmAkkaSerializable

import scala.collection.immutable.IndexedSeq

sealed trait SequenceManagerState extends EnumEntry with SmAkkaSerializable
object SequenceManagerState extends Enum[SequenceManagerState] {

  override def values: IndexedSeq[SequenceManagerState] = findValues

  case object Idle                          extends SequenceManagerState
  case object Configuring                   extends SequenceManagerState
  case object CleaningUp                    extends SequenceManagerState
  case object StartingSequencer             extends SequenceManagerState
  case object ShuttingDownSequencer         extends SequenceManagerState
  case object ShuttingDownAllSequencers     extends SequenceManagerState
  case object RestartingSequencer           extends SequenceManagerState
  case object ShuttingDownSequenceComponent extends SequenceManagerState
}
