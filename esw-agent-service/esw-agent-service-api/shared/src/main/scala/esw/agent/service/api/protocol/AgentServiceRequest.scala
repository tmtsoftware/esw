package esw.agent.service.api.protocol

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix

import java.nio.file.Path

sealed trait AgentServiceRequest

object AgentServiceRequest {

  case class SpawnSequenceManager(agentPrefix: Prefix, obsModeConfigPath: Path, isConfigLocal: Boolean, version: Option[String])
      extends AgentServiceRequest

  case class SpawnSequenceComponent(agentPrefix: Prefix, componentName: String, version: Option[String])
      extends AgentServiceRequest

  case class SpawnContainers(agentPrefix: Prefix, hostConfigPath: String, isConfigLocal: Boolean) extends AgentServiceRequest

  case class KillComponent(componentId: ComponentId) extends AgentServiceRequest
}
