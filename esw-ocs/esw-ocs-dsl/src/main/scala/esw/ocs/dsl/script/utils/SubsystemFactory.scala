package esw.ocs.dsl.script.utils

import csw.params.core.models.Subsystem

object SubsystemFactory {
  def make(name: String): Subsystem = Subsystem.withNameInsensitive(name)
}
