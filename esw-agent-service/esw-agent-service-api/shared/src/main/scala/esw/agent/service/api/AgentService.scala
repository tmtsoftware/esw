package esw.agent.service.api

import java.nio.file.Path

import csw.location.api.models.Connection
import csw.prefix.models.Prefix
import esw.agent.service.api.models.{KillResponse, SpawnResponse}

import scala.concurrent.Future

trait AgentService {

  def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String] = None
  ): Future[SpawnResponse]

  def spawnSequenceComponent(agentPrefix: Prefix, componentName: String, version: Option[String] = None): Future[SpawnResponse]

  def killComponent(connection: Connection): Future[KillResponse]
}
