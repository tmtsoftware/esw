package esw.services

import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.Await

object Agent {

  def service(enable: Boolean, agentPrefix: Option[Prefix]): ManagedService[AgentWiring] =
    ManagedService("agent", enable, () => startAgent(agentPrefix.getOrElse(Prefix(ESW, "primary"))), stopAgent)

  private def startAgent(prefix: Prefix): AgentWiring = {
    AgentApp.start(AgentSettings(prefix, ConfigFactory.load()))
  }

  private def stopAgent(wiring: AgentWiring): Unit = {
    wiring.actorSystem.terminate()
    Await.result(wiring.actorSystem.whenTerminated, CommonTimeouts.Wiring)
  }
}
