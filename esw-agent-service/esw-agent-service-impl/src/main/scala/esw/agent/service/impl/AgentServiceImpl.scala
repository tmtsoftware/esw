package esw.agent.service.impl

import akka.actor.typed.ActorSystem
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil

import java.nio.file.Path
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
    val compLocation = locationServiceUtil.findAkkaLocation(componentId.prefix.toString(), componentId.componentType)

    val agentLocation = compLocation.flatMapE { location =>
      locationServiceUtil.findAgentByHostname(location.uri.getHost)
    }
    val agentClient = agentLocation.mapRight(location => makeAgentClient(location))
    compLocation
      .flatMapE { cl => agentClient.flatMapRight { c => c.killComponent(cl) } }
      .mapToAdt(identity, e => Failed(e.msg))
  }

  override def getAgentStatus: Future[AgentStatusResponse] = agentStatusUtil.getAllAgentStatus

  private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
    AgentClient.make(agentPrefix, locationServiceUtil).mapLeft(e => e.msg)

  private[impl] def makeAgentClient(akkaLocation: AkkaLocation): AgentClient = new AgentClient(akkaLocation)

  private def getAgentPrefix(location: Location): Either[String, Prefix] =
    location.metadata.getAgentPrefix.toRight(s"$location metadata does not contain agent prefix")
}
