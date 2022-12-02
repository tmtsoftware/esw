package esw.ocs.testkit.utils

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import esw.agent.service.app.{AgentServiceApp, AgentServiceWiring}

trait AgentServiceUtils {
  implicit def actorSystem: ActorSystem[SpawnProtocol.Command]

  private var agentServiceWiring: Option[AgentServiceWiring] = None

  def spawnAgentService(): Unit = {
    agentServiceWiring = Some(AgentServiceApp.StartCommand.start(None, startLogging = false))
  }

  def shutdownAgentService(): Unit = agentServiceWiring.foreach(_.actorRuntime.shutdown(UnknownReason))
}
