package esw.agent.http.api

import java.nio.file.Path

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
import esw.agent.api.{KillResponse, SpawnResponse}

import scala.concurrent.Future

trait AgentService {

  def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String] = None
  ): Future[SpawnResponse]

  def spawnSequenceComponent(agentPrefix: Prefix, prefix: Prefix, version: Option[String] = None): Future[SpawnResponse]

  def stopComponent(agentPrefix: Prefix, componentId: ComponentId): Future[KillResponse]
}
