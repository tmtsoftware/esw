package esw.agent.service.impl

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import csw.location.api.models.{ComponentId, Location}
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models.{Failed, KillResponse, Killed, SpawnResponse}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil

import scala.concurrent.{ExecutionContext, Future}

class AgentServiceImpl(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) extends AgentServiceApi {

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

  override def killComponent(componentId: ComponentId): Future[KillResponse] =
    locationServiceUtil
      .list(componentId)
      .flatMap { locL =>
        Future.traverse(locL) { location =>
          Future.successful(getAgentPrefix(location)).flatMapE(agentClient).flatMapRight(_.killComponent(location))
        }
      }
      .map {
        case responses if responses.forall(_ == Right(Killed)) => Right(Killed)
        case responses                                         => responses.find(_ != Right(Killed)).get
      }
      .mapToAdt(identity, Failed)

  private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
    AgentClient.make(agentPrefix, locationServiceUtil).mapLeft(e => e.msg)

  private def getAgentPrefix(location: Location): Either[String, Prefix] =
    location.metadata.getAgentPrefix.toRight(s"$location metadata does not contain agent prefix")
}
