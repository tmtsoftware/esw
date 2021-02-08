package esw.sm.impl.config

import esw.ocs.api.models.ObsMode
import esw.sm.api.models.{Resources, Sequencers}

case class ObsModeConfig(resources: Resources, sequencers: Sequencers)

case class SequenceManagerConfig(obsModes: Map[ObsMode, ObsModeConfig]) {
  def resources(obsMode: ObsMode): Option[Resources]         = obsModeConfig(obsMode).map(_.resources)
  def sequencers(obsMode: ObsMode): Option[Sequencers]       = obsModeConfig(obsMode).map(_.sequencers)
  def obsModeConfig(obsMode: ObsMode): Option[ObsModeConfig] = obsModes.get(obsMode)
}
