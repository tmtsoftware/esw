package esw.smSimulation.app

import java.nio.file.Path

import com.typesafe.config.ConfigFactory
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.agent.akka.app.{AgentApp, AgentSettings}
import esw.commons.utils.location.EswLocationError
import esw.sm.app.SequenceManagerWiring

class SequenceManagerSimulationWiring(obsModeConfigPath: Path, isLocal: Boolean = true, agentPrefix: Option[Prefix])
    extends SequenceManagerWiring(obsModeConfigPath, isLocal, agentPrefix) {

  // spawn agent
  def spawnAgent(agentPrefix: Prefix): Unit = {
    val agentConfig   = ConfigFactory.load()
    val agentSettings = AgentSettings(agentPrefix, agentConfig)
    println(">>>>>>>>>>>>>>>>>>>>> coursier channel" + agentSettings.coursierChannel)
    AgentApp.start(agentSettings)
  }

  def startSimulation(): Either[EswLocationError.RegistrationError, AkkaLocation] = {
    spawnAgent(Prefix(ESW, "machine1"))
    spawnAgent(Prefix(TCS, "machine1"))
    spawnAgent(Prefix(IRIS, "machine1"))
    start()
  }

}
