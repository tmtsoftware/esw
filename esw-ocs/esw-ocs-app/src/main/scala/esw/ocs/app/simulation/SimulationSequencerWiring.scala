package esw.ocs.app.simulation

import csw.location.api.models.AkkaLocation
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.ocs.app.wiring.{SequencerConfig, SequencerWiring}
import esw.ocs.impl.script.ScriptApi

class SimulationSequencerWiring(
    override val subsystem: Subsystem,
    override val observingMode: ObsMode,
    sequenceComponentLocation: AkkaLocation,
    simulationScript: SimulationScript = SimulationScript
) extends SequencerWiring(subsystem, observingMode, sequenceComponentLocation) {

  private val heartbeatInterval      = config.getDuration("esw.heartbeat-interval")
  private val enableThreadMonitoring = config.getBoolean("esw.enable-thread-monitoring")

  override private[ocs] lazy val sequencerConfig =
    SequencerConfig(
      Prefix(s"$subsystem.${observingMode.name}"),
      simulationScript.getClass.getName,
      heartbeatInterval,
      enableThreadMonitoring
    )

  override private[ocs] lazy val script: ScriptApi = simulationScript
}
