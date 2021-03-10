package esw.shell.service

import csw.location.api.models.ComponentType
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.SpawnContainersResponse
import esw.commons.utils.files.FileUtils

import scala.concurrent.Future

object Container {

  def spawnSimulatedComponent(
      prefix: String,
      `type`: ComponentType,
      agentClient: AgentClient
  ): Future[SpawnContainersResponse] = {
    val standaloneConf =
      s"""
         |prefix = $prefix
         |componentType = ${`type`}
         |behaviorFactoryClassName = "esw.shell.component.SimulatedComponentBehaviourFactory"
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
         |    mode: "Standalone"
         |    configFilePath: "$standaloneConfPath"
         |    configFileLocation: "Local"
         |  }
         |]
         |""".stripMargin
    val hostConfigPath = FileUtils.createTempConfFile("hostConfig", hostConf).toString
    agentClient.spawnContainers(hostConfigPath, isConfigLocal = true)
  }
}
