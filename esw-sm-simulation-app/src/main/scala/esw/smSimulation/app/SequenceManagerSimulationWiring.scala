package esw.smSimulation.app

import java.nio.file.Path

import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.agent.akka.app.{AgentApp, AgentSettings}
import esw.sm.app.SequenceManagerWiring

class SequenceManagerSimulationWiring(obsModeConfigPath: Path, isLocal: Boolean = true, agentPrefix: Option[Prefix])
    extends SequenceManagerWiring(obsModeConfigPath, isLocal, agentPrefix) {

  // spawn agent
  def spawnAgent(agentPrefix: Prefix): Unit = {
    val agentSettings = AgentSettings(agentPrefix, ConfigFactory.load())
    AgentApp.start(agentSettings)
  }

  def startSimulation() = {
    spawnAgent(Prefix(ESW, "machine1"))
    spawnAgent(Prefix(TCS, "machine1"))
    spawnAgent(Prefix(IRIS, "machine1"))
    start()
  }

}
