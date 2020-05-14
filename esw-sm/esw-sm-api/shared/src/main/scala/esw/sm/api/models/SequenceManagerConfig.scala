package esw.sm.api.models

import csw.prefix.models.Subsystem

case class Resources(resources: Set[String]) {
  def conflictsWith(other: Resources): Boolean = {
    this.resources.exists(r => other.resources.contains(r))
  }
}
object Resources {
  def apply(resources: String*): Resources = new Resources(resources.toSet)
}

case class Sequencers(subsystems: List[Subsystem])
object Sequencers {
  def apply(subsystems: Subsystem*): Sequencers = new Sequencers(subsystems.toList)
}

case class ObsModeConfig(resources: Resources, sequencers: Sequencers)

case class SequenceManagerConfig(obsModes: Map[String, ObsModeConfig]) {
  def resources(obsMode: String): Option[Resources]         = obsModeConfig(obsMode).map(_.resources)
  def sequencers(obsMode: String): Option[Sequencers]       = obsModeConfig(obsMode).map(_.sequencers)
  def obsModeConfig(obsMode: String): Option[ObsModeConfig] = obsModes.get(obsMode)
}
