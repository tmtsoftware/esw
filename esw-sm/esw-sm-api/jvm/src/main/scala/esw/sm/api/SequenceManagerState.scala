package esw.sm.api

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed trait SequenceManagerState extends EnumEntry
object SequenceManagerState extends Enum[SequenceManagerState] {

  override def values: IndexedSeq[SequenceManagerState] = findValues

  case object Idle                   extends SequenceManagerState
  case object ConfigurationInProcess extends SequenceManagerState
  case object CleaningInProcess      extends SequenceManagerState
}
