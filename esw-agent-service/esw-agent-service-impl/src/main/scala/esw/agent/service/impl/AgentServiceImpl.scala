package esw.agent.service.impl

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentId
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models.{AgentNotFoundException, Failed, KillResponse, SpawnResponse}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

class AgentServiceImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AgentServiceApi {

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  private[impl] def agentClient(agentPrefix: Prefix): Future[Either[Failed, AgentClient]] =
    AgentClient.make(agentPrefix, locationService).map(Right(_)).recover {
      case AgentNotFoundException(msg) => Left(Failed(msg))
    }

  override def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String]
  ): Future[SpawnResponse] =
    agentClient(agentPrefix)
      .flatMapRight(_.spawnSequenceManager(obsModeConfigPath, isConfigLocal, version))
      .mapToAdt(identity, identity)

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] =
    agentClient(agentPrefix)
      .flatMapRight(_.spawnSequenceComponent(componentName, version))
      .mapToAdt(identity, identity)

  override def killComponent(agentPrefix: Prefix, componentId: ComponentId): Future[KillResponse] =
    agentClient(agentPrefix).flatMapRight(_.killComponent(componentId)).mapToAdt(identity, identity)

}
