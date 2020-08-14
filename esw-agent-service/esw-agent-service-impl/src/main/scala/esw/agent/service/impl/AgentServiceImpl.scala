package esw.agent.service.impl

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import csw.location.api.models.{ComponentId, Connection, Location}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentService
import esw.agent.service.api.models.{Failed, KillResponse, SpawnResponse}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class AgentServiceImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AgentService {

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  private val locationServiceUtil           = new LocationServiceUtil(locationService)

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

  override def killComponent(agentPrefix: Prefix, componentId: ComponentId): Future[KillResponse] =
    agentClient(agentPrefix).flatMapRight(_.killComponent(componentId)).mapToAdt(identity, Failed)

  def killComponent0(connection: Connection): Future[KillResponse] =
    getAgentClient(connection).flatMapRight(_.killComponent(connection.componentId)).mapToAdt(identity, Failed)


  private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
    AgentClient.make(agentPrefix, locationService).transform(x => Success(x.toEither.left.map(_.getMessage)))

  private def getComponentLocation(connection: Connection): Future[Either[String, Location]] =
    locationServiceUtil.find(connection.of[Location]).mapLeft(_.msg)

  private def getAgentPrefix(location: Location): Either[String, Prefix] =
    location.metadata.metadata.get("agent-prefix").toRight(s"$location does not contain agent prefix").flatMap(makePrefix)

  private def getAgentClient(connection: Connection): Future[Either[String, AgentClient]] =
    getComponentLocation(connection).mapRightE(getAgentPrefix).flatMapE(agentClient)

  private def makePrefix(prefix: String): Either[String, Prefix] =
    Try(Prefix(prefix)).toEither.left.map(_.getMessage)

  //  private[impl] def agentClient0(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
  //    agentClient(agentPrefix)
  //      .map(Right(_))
  //      .recover{
  //      case NonFatal(e) => Left(e.getMessage)
  //    }

}

//  private def getAgentClient(connection: Connection): Future[Option[AgentClient]] = {
//    val compLocF: Future[Option[Location]] = getComponentLocation(connection)
//    val agentPrefixF: Future[Option[Prefix]] = compLocF.map(_.flatMap(getAgentPrefix))
//
//    val agentClientF: Future[Option[AgentClient]] = agentPrefixF.flatMap {
//      case Some(prefix) => agentClient(prefix).map(Some(_))
//      case None => Future.successful(None)
//    }
//    agentClientF
//  }
