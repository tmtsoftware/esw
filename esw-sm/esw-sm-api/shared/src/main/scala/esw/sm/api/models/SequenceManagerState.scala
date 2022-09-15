/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.api.models

import enumeratum.{Enum, EnumEntry}
import esw.sm.api.codecs.SmAkkaSerializable

import scala.collection.immutable.IndexedSeq

sealed trait SequenceManagerState extends EnumEntry with SmAkkaSerializable
object SequenceManagerState extends Enum[SequenceManagerState] {

  override def values: IndexedSeq[SequenceManagerState] = findValues

  case object Idle       extends SequenceManagerState
  case object Processing extends SequenceManagerState
}
