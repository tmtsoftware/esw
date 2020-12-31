package esw.services

import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}

object Agent {

  def service(enable: Boolean, agentPrefix: Option[Prefix]): ManagedService[AgentWiring] =
    ManagedService("agent", enable, () => startAgent(agentPrefix), stopAgent)

  private def startAgent(maybePrefix: Option[Prefix]): Option[AgentWiring] = {
    maybePrefix.map(p => AgentApp.start(AgentSettings(p, ConfigFactory.load())))
  }

  private def stopAgent(wiring: AgentWiring): Unit = {
    wiring.actorSystem.terminate()
    wiring.actorSystem.whenTerminated
  }
}
