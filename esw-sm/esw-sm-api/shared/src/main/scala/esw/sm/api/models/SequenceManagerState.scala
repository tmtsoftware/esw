package esw.sm.api.models

import enumeratum.{Enum, EnumEntry}
import esw.sm.api.codecs.SmPekkoSerializable

import scala.collection.immutable.IndexedSeq

sealed trait SequenceManagerState extends EnumEntry with SmPekkoSerializable
object SequenceManagerState extends Enum[SequenceManagerState] {

  override def values: IndexedSeq[SequenceManagerState] = findValues

  case object Idle       extends SequenceManagerState
  case object Processing extends SequenceManagerState
}
