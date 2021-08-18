package esw.agent.service.api.client

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models.{AgentStatusResponse, KillResponse, SpawnContainersResponse, SpawnResponse}
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{
  GetAgentStatus,
  KillComponent,
  SpawnContainers,
  SpawnSequenceComponent,
  SpawnSequenceManager
}
import msocket.api.Transport
import java.nio.file.Path

import scala.concurrent.Future

/**
 * HTTP client for the Agent Service
 *
 * @param postClient - An Transport class for HTTP calls for the Agent Service
 */
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

  override def spawnContainers(
      agentPrefix: Prefix,
      hostConfigPath: String,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] =
    postClient.requestResponse[SpawnContainersResponse](SpawnContainers(agentPrefix, hostConfigPath, isConfigLocal))

  override def killComponent(componentId: ComponentId): Future[KillResponse] =
    postClient.requestResponse[KillResponse](KillComponent(componentId))

  override def getAgentStatus: Future[AgentStatusResponse] = postClient.requestResponse[AgentStatusResponse](GetAgentStatus)
}
