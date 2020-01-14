package esw.ocs.dsl.script.utils

import csw.prefix.models.Subsystem

object SubsystemFactory {
  def make(name: String): Subsystem = Subsystem.withNameInsensitive(name)
}
