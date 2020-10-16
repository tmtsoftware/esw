package esw.agent.service.api.client

import java.nio.file.Path

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models.{KillResponse, SpawnResponse}
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{KillComponent, SpawnSequenceComponent, SpawnSequenceManager}
import msocket.api.Transport

import scala.concurrent.Future

class AgentServiceClient(postClient: Transport[AgentServiceRequest]) extends AgentServiceApi with AgentServiceCodecs {

  override def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String]
  ): Future[SpawnResponse] =
    postClient.requestResponse[SpawnResponse](SpawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version))

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] =
    postClient.requestResponse[SpawnResponse](SpawnSequenceComponent(agentPrefix, componentName, version))

  override def killComponent(componentId: ComponentId): Future[KillResponse] =
    postClient.requestResponse[KillResponse](KillComponent(componentId))
}
