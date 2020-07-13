package esw.sm.impl.config

import csw.prefix.models.Subsystem
import esw.ocs.api.models.ObsMode

case class Resource(subsystem: Subsystem)

case class Resources(resources: Set[Resource]) {
  private def conflictsWith(other: Resources): Boolean  = this.resources.exists(other.resources.contains)
  def conflictsWithAny(others: Set[Resources]): Boolean = others.exists(conflictsWith)
}
object Resources {
  def apply(resources: Resource*): Resources = new Resources(resources.toSet)
}

case class Sequencers(subsystems: List[Subsystem])
object Sequencers {
  def apply(subsystems: Subsystem*): Sequencers = new Sequencers(subsystems.toList)
}

case class ObsModeConfig(resources: Resources, sequencers: Sequencers)

case class SequenceManagerConfig(obsModes: Map[ObsMode, ObsModeConfig]) {
  def resources(obsMode: ObsMode): Option[Resources]         = obsModeConfig(obsMode).map(_.resources)
  def sequencers(obsMode: ObsMode): Option[Sequencers]       = obsModeConfig(obsMode).map(_.sequencers)
  def obsModeConfig(obsMode: ObsMode): Option[ObsModeConfig] = obsModes.get(obsMode)
}

case class ProvisionConfig(config: Map[Subsystem, Int])
