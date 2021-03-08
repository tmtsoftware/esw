package esw.services.apps

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import com.typesafe.config.Config
import csw.prefix.models.Prefix
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.Await

object Agent {

  def service(
      enable: Boolean,
      agentPrefix: Prefix,
      agentConfig: Config,
      hostConfigPath: Option[String] = None
  ): ManagedService[AgentWiring] =
    ManagedService("agent", enable, () => startAgent(agentPrefix, agentConfig, hostConfigPath), stopAgent)

  private def startAgent(prefix: Prefix, agentConfig: Config, hostConfigPath: Option[String]): AgentWiring =
    AgentApp.start(AgentSettings(prefix, agentConfig), hostConfigPath, isConfigLocal = true)

  private def stopAgent(wiring: AgentWiring): Unit =
    Await.result(wiring.actorRuntime.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
