package esw.gateway.server.restless.messages

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait CommandAction extends EnumEntry

object CommandAction extends Enum[CommandAction] {
  case object Validate extends CommandAction
  case object Submit   extends CommandAction
  case object Oneway   extends CommandAction

  override def values: immutable.IndexedSeq[CommandAction] = findValues
}
