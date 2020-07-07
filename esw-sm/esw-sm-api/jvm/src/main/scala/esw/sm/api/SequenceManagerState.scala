package esw.sm.api

import enumeratum.{Enum, EnumEntry}
import esw.sm.api.codecs.SmAkkaSerializable

import scala.collection.immutable.IndexedSeq

sealed trait SequenceManagerState extends EnumEntry with SmAkkaSerializable
object SequenceManagerState extends Enum[SequenceManagerState] {

  override def values: IndexedSeq[SequenceManagerState] = findValues

  case object Idle       extends SequenceManagerState
  case object Processing extends SequenceManagerState
}
