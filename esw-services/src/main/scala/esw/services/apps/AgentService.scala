package esw.services.apps

import esw.agent.service.app.{AgentServiceApp, AgentServiceWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.Await

object AgentService {

  def service(enable: Boolean): ManagedService[AgentServiceWiring] =
    ManagedService("agent-service", enable, () => startAgentService(), stopAgentService)

  private def startAgentService(): AgentServiceWiring = AgentServiceApp.start()

  private def stopAgentService(wiring: AgentServiceWiring): Unit =
    Await.result(wiring.stop(), CommonTimeouts.Wiring)
}
