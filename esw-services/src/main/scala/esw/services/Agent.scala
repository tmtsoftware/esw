package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.Await

object Agent {

  def service(enable: Boolean, agentPrefix: Prefix): ManagedService[AgentWiring] =
    ManagedService("agent", enable, () => startAgent(agentPrefix), stopAgent)

  private def startAgent(prefix: Prefix): AgentWiring = AgentApp.start(AgentSettings(prefix, ConfigFactory.load()))

  private def stopAgent(wiring: AgentWiring): Unit =
    Await.result(wiring.actorRuntime.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
