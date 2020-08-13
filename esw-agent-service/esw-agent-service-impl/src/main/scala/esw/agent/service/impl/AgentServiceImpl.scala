package esw.agent.service.impl

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentId
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentService
import esw.agent.service.api.models.{KillResponse, SpawnResponse}

import scala.concurrent.{ExecutionContext, Future}

class AgentServiceImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AgentService {

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  private[impl] def agentClient(agentPrefix: Prefix): Future[AgentClient] = AgentClient.make(agentPrefix, locationService)

  override def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String]
  ): Future[SpawnResponse] =
    agentClient(agentPrefix).flatMap(_.spawnSequenceManager(obsModeConfigPath, isConfigLocal, version))

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] =
    agentClient(agentPrefix).flatMap(_.spawnSequenceComponent(Prefix(agentPrefix.subsystem, componentName), version))

  override def killComponent(agentPrefix: Prefix, componentId: ComponentId): Future[KillResponse] =
    agentClient(agentPrefix).flatMap(_.killComponent(componentId))
}
