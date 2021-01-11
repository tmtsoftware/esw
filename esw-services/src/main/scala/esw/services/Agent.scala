package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.Await

object Agent {
  def DefaultAgentPrefix: Prefix = Prefix(ESW, "primary")

  def service(enable: Boolean, agentPrefix: Option[Prefix]): ManagedService[AgentWiring] =
    ManagedService("agent", enable, () => startAgent(agentPrefix.getOrElse(DefaultAgentPrefix)), stopAgent)

  private def startAgent(prefix: Prefix): AgentWiring = AgentApp.start(AgentSettings(prefix, ConfigFactory.load()))

  private def stopAgent(wiring: AgentWiring): Unit =
    Await.result(wiring.actorRuntime.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
