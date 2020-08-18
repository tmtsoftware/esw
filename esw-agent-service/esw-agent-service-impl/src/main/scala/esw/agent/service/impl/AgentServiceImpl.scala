package esw.agent.service.impl

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import csw.location.api.models.{Connection, Location}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models.{Failed, KillResponse, SpawnResponse}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class AgentServiceImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AgentServiceApi {

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  private val locationServiceUtil = new LocationServiceUtil(locationService)

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

  override def killComponent(connection: Connection): Future[KillResponse] =
    getComponentLocation(connection)
      .flatMapE { location =>
        Future.successful(getAgentPrefix(location)).flatMapE(agentClient).flatMapRight(_.killComponent(location))
      }
      .mapToAdt(identity, Failed)

  private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
    AgentClient.make(agentPrefix, locationService).transform(client => Success(client.toEither.left.map(_.getMessage)))

  private def getComponentLocation(connection: Connection): Future[Either[String, Location]] =
    locationServiceUtil.find(connection.of[Location]).mapLeft(_.msg)

  private def getAgentPrefix(location: Location): Either[String, Prefix] =
    location.metadata.getAgentPrefix.toRight(s"$location metadata does not contain agent prefix")

}
