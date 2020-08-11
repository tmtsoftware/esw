package esw.agent.http.client

import java.nio.file.Path

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
import esw.agent.api.protocol.AgentPostRequest
import esw.agent.api.protocol.AgentPostRequest.{SpawnSequenceComponent, SpawnSequenceManager, StopComponent}
import esw.agent.api.{KillResponse, SpawnResponse}
import esw.agent.http.api.AgentService
import msocket.api.Transport

import scala.concurrent.Future

class AgentServiceClient(postClient: Transport[AgentPostRequest]) extends AgentService {

  override def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String]
  ): Future[SpawnResponse] =
    postClient.requestResponse(SpawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version))

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] =
    postClient.requestResponse(SpawnSequenceComponent(agentPrefix, componentName, version))

  override def stopComponent(agentPrefix: Prefix, componentId: ComponentId): Future[KillResponse] =
    postClient.requestResponse(StopComponent(agentPrefix, componentId))
}
