package esw.services.apps

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import com.typesafe.config.Config
import csw.prefix.models.Prefix
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import java.nio.file.Paths
import scala.concurrent.Await

object Agent {

  def service(enable: Boolean, agentPrefix: Prefix, agentConfig: Config): ManagedService[AgentWiring] =
    ManagedService("agent", enable, () => startAgent(agentPrefix, agentConfig), stopAgent)

  //TODO: Fix host config path and configLocal args
  private def startAgent(prefix: Prefix, agentConfig: Config): AgentWiring =
    AgentApp.start(AgentSettings(prefix, agentConfig), Paths.get(""), isConfigLocal = true)

  private def stopAgent(wiring: AgentWiring): Unit =
    Await.result(wiring.actorRuntime.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
