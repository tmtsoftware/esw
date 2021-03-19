package esw.agent.service.impl

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import csw.location.api.models.{ComponentId, Location}
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.location.LocationServiceUtil

import scala.concurrent.{ExecutionContext, Future}

class AgentServiceImpl(locationServiceUtil: LocationServiceUtil, agentStatusUtil: AgentStatusUtil)(implicit
    actorSystem: ActorSystem[_]
) extends AgentServiceApi {

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  override def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String]
  ): Future[SpawnResponse] =
    agentClient(agentPrefix)
      .flatMapRight(_.spawnSequenceManager(obsModeConfigPath, isConfigLocal, version))
      .mapToAdt(identity, Failed)

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] =
    agentClient(agentPrefix)
      .flatMapRight(_.spawnSequenceComponent(componentName, version))
      .mapToAdt(identity, Failed)

  override def spawnContainers(
      agentPrefix: Prefix,
      hostConfigPath: String,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] =
    agentClient(agentPrefix).flatMapRight(_.spawnContainers(hostConfigPath, isConfigLocal)).mapToAdt(identity, Failed)

  override def killComponent(componentId: ComponentId): Future[KillResponse] = {
    locationServiceUtil
      .list(componentId)
      .flatMap { locations =>
        Future.traverse(locations) { location =>
          Future.successful(getAgentPrefix(location)).flatMapE(agentClient).flatMapRight(_.killComponent(location))
        }
      }
      .map { responses =>
        responses.sequence.map(_ => Killed).left.map(_.mkString(","))
      }
      .mapToAdt(identity, Failed)
  }

  override def getAgentStatus: Future[AgentStatusResponse] = agentStatusUtil.getAllAgentStatus

  private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
    AgentClient.make(agentPrefix, locationServiceUtil).mapLeft(e => e.msg)

  private def getAgentPrefix(location: Location): Either[String, Prefix] =
    location.metadata.getAgentPrefix.toRight(s"$location metadata does not contain agent prefix")
}
