package esw.ocs.app.simulation

import csw.prefix.models.Prefix
import esw.ocs.app.wiring.{SequencerConfig, SequencerWiring}
import esw.ocs.impl.script.ScriptApi

/*
 * wiring - this wiring is being used while starting the sequencer in simulation mode
 * to use the simulation script and ignore the script(present in conf with mapping of subsystem and observing mode) which is to be loaded
 */
class SimulationSequencerWiring(
    override val sequencerPrefix: Prefix,
    sequenceComponentPrefix: Prefix,
    simulationScript: SimulationScript = SimulationScript
) extends SequencerWiring(sequencerPrefix, sequenceComponentPrefix) {

  private val heartbeatInterval      = config.getDuration("esw.heartbeat-interval")
  private val enableThreadMonitoring = config.getBoolean("esw.enable-thread-monitoring")

  override private[ocs] lazy val sequencerConfig =
    SequencerConfig(
      sequencerPrefix,
      simulationScript.getClass.getName,
      heartbeatInterval,
      enableThreadMonitoring
    )

  override private[ocs] lazy val script: ScriptApi = simulationScript
}
