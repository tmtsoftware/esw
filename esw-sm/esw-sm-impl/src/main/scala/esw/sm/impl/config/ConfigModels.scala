package esw.sm.impl.config

import csw.prefix.codecs.CommonCodecs
import csw.prefix.models.Subsystem
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec

case class Resources(resources: Set[String]) {
  private def conflictsWith(other: Resources): Boolean  = this.resources.exists(other.resources.contains)
  def conflictsWithAny(others: Set[Resources]): Boolean = others.exists(conflictsWith)
}
object Resources {
  def apply(resources: String*): Resources = new Resources(resources.toSet)
}

case class Sequencers(subsystems: List[Subsystem])
object Sequencers {
  def apply(subsystems: Subsystem*): Sequencers = new Sequencers(subsystems.toList)
}

case class ObsModeConfig(resources: Resources, sequencers: Sequencers)

case class SequenceManagerConfig(obsModes: Map[String, ObsModeConfig], sequencerStartRetries: Int) {
  def resources(obsMode: String): Option[Resources]         = obsModeConfig(obsMode).map(_.resources)
  def sequencers(obsMode: String): Option[Sequencers]       = obsModeConfig(obsMode).map(_.sequencers)
  def obsModeConfig(obsMode: String): Option[ObsModeConfig] = obsModes.get(obsMode)
}

object ConfigCodecs extends CommonCodecs {
  // Codecs for SequenceManagerConfig to parse config json string to domain model using borer
  implicit lazy val obsModeConfigCodec: Codec[ObsModeConfig]                 = deriveCodec
  implicit lazy val resourcesCodec: Codec[Resources]                         = deriveCodec
  implicit lazy val sequencersCodec: Codec[Sequencers]                       = deriveCodec
  implicit lazy val sequenceManagerConfigCodec: Codec[SequenceManagerConfig] = deriveCodec
}
