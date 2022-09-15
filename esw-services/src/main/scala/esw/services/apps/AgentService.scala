/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.services.apps

import esw.agent.service.app.{AgentServiceApp, AgentServiceWiring}
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.Await

// This class is created to start and stop the Agent Service
object AgentService {

  // Creates an instance of ManagedService with start and stop hook for the Agent Service
  def service(enable: Boolean): ManagedService[AgentServiceWiring] =
    ManagedService("agent-service", enable, () => startAgentService(), stopAgentService)

  // This method is for starting the Agent Service and being called in the start hook for the Agent Service
  private def startAgentService(): AgentServiceWiring = AgentServiceApp.start(None)

  // This method is for stopping the Agent Service and being called in the stop hook for the Agent Service
  private def stopAgentService(wiring: AgentServiceWiring): Unit =
    Await.result(wiring.stop(), CommonTimeouts.Wiring)
}
