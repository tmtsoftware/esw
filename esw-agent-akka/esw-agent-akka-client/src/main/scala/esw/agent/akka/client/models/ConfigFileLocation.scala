package esw.agent.akka.client.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait ConfigFileLocation extends EnumEntry

object ConfigFileLocation extends Enum[ConfigFileLocation] {
  override def values: immutable.IndexedSeq[ConfigFileLocation] = findValues

  case object Local  extends ConfigFileLocation
  case object Remote extends ConfigFileLocation
}
