package esw.services.apps

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import com.typesafe.config.Config
import csw.prefix.models.Prefix
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.Await

// This class is created to start and stop any given agent
object Agent {

  // Creates an instance of ManagedService with start and stop hook for the given agent
  def service(
      enable: Boolean,
      //prefix of the agent to be started
      agentPrefix: Prefix,
      agentConfig: Config,
      hostConfigPath: Option[String] = None
  ): ManagedService[AgentWiring] =
    ManagedService("agent", enable, () => startAgent(agentPrefix, agentConfig, hostConfigPath), stopAgent)

  //This method is for starting the agent and being called in the start hook for the Agent
  private def startAgent(prefix: Prefix, agentConfig: Config, hostConfigPath: Option[String]): AgentWiring =
    AgentApp.start(AgentSettings(prefix, agentConfig), hostConfigPath, isConfigLocal = true)

  //This method is for stopping the agent and being called in the stop hook for the Agent
  private def stopAgent(wiring: AgentWiring): Unit =
    Await.result(wiring.actorRuntime.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
