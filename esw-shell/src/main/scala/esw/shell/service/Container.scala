/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.shell.service

import csw.location.api.models.ComponentType
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.SpawnContainersResponse
import esw.commons.utils.files.FileUtils

import scala.concurrent.Future

object Container {

  /**
   * Spawns a simulated component inside a container.
   *
   * @param prefix - prefix of the component
   * @param componentType - component type of the component(HCD or Assembly)
   * @param agentClient - client of the agent on which component(inside a container) to be spawned
   *
   * @return a [[esw.agent.service.api.models.SpawnContainersResponse]] response as Future value
   */
  def spawnSimulatedComponent(
      prefix: String,
      componentType: ComponentType,
      agentClient: AgentClient
  ): Future[SpawnContainersResponse] = {
    val standaloneConf =
      s"""
         |prefix = $prefix
         |componentType = $componentType
         |componentHandlerClassName = "esw.shell.component.SimulatedComponentHandlers"
         |locationServiceUsage = RegisterOnly
         |""".stripMargin
    val standaloneConfPath = FileUtils.createTempConfFile("standalone", standaloneConf).toString
    val hostConf =
      s"""
         |containers: [
         |  {
         |    orgName: "com.github.tmtsoftware.esw"
         |    deployModule: "esw-shell"
         |    appName: "esw.shell.component.SimulatedContainerCmdApp"
         |    version: "0.1.0-SNAPSHOT"
         |    configFilePath: "$standaloneConfPath"
         |    configFileLocation: "Local"
         |  }
         |]
         |""".stripMargin
    val hostConfigPath = FileUtils.createTempConfFile("hostConfig", hostConf).toString
    agentClient.spawnContainers(hostConfigPath, isConfigLocal = true)
  }
}
