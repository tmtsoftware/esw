package esw.sm.core

import csw.prefix.models.Subsystem

case class Resources(resources: List[String]) {
  def conflictsWith(other: Resources): Boolean = {
    this.resources.exists(r => other.resources.contains(r))
  }
}

case class Sequencers(subsystems: List[Subsystem])
