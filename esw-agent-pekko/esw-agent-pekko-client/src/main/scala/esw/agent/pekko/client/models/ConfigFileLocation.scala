package esw.agent.pekko.client.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait ConfigFileLocation extends EnumEntry

/**
 * ADT representation of config file location.
 */
object ConfigFileLocation extends Enum[ConfigFileLocation] {
  override def values: immutable.IndexedSeq[ConfigFileLocation] = findValues

  /**
   * This represents file present in resource folder or on the same machine if the given path is absolute.
   */
  case object Local extends ConfigFileLocation

  /**
   * This represents file present in the configuration service.
   */
  case object Remote extends ConfigFileLocation
}
