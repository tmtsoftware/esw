package esw.sm.api.models

import csw.prefix.models.Subsystem
import esw.sm.api.models.CommonFailure.ConfigurationMissing

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

  def resources(obsMode: String): Either[ConfigurationMissing, Resources] = {
    obsModeConfig(obsMode).map(_.resources)
  }

  def sequencers(obsMode: String): Either[ConfigurationMissing, Sequencers] = {
    obsModeConfig(obsMode).map(_.sequencers)
  }

  private def obsModeConfig(obsMode: String): Either[ConfigurationMissing, ObsModeConfig] = {
    obsModes.get(obsMode) match {
      case Some(obsModeConfig) => Right(obsModeConfig)
      case None                => Left(ConfigurationMissing(obsMode))
    }
  }
}
