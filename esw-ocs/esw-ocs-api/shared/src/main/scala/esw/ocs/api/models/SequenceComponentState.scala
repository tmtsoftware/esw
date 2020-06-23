package esw.ocs.api.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed trait SequenceComponentState extends EnumEntry
object SequenceComponentState extends Enum[SequenceComponentState] {

  override def values: IndexedSeq[SequenceComponentState] = findValues

  case object Idle    extends SequenceComponentState
  case object Running extends SequenceComponentState
}
