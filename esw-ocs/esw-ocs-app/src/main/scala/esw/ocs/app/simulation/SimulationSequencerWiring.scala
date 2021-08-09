package esw.ocs.app.simulation

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.ocs.app.wiring.{SequencerConfig, SequencerWiring}
import esw.ocs.impl.script.ScriptApi

/*
 * wiring - this wiring is being used while starting the sequencer in simulation mode
 * to use the simulation script and ignore the script(present in conf with mapping of subsystem and observing mode) which is to be loaded
 */
class SimulationSequencerWiring(
    override val subsystem: Subsystem,
    override val obsMode: ObsMode,
    sequenceComponentPrefix: Prefix,
    simulationScript: SimulationScript = SimulationScript
) extends SequencerWiring(subsystem, obsMode, sequenceComponentPrefix) {

  private val heartbeatInterval      = config.getDuration("esw.heartbeat-interval")
  private val enableThreadMonitoring = config.getBoolean("esw.enable-thread-monitoring")

  override private[ocs] lazy val sequencerConfig =
    SequencerConfig(
      Prefix(s"$subsystem.${obsMode.name}"),
      simulationScript.getClass.getName,
      heartbeatInterval,
      enableThreadMonitoring
    )

  override private[ocs] lazy val script: ScriptApi = simulationScript
}
