package esw.agent.akka.client.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait ContainerMode extends EnumEntry

/**
 * ADT representation of different modes for spawning HCD Or Assemblies.
 */
object ContainerMode extends Enum[ContainerMode] {
  override def values: immutable.IndexedSeq[ContainerMode] = findValues

  final case object Container  extends ContainerMode
  final case object Standalone extends ContainerMode
}
