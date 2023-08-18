package esw.agent.service.impl

import org.apache.pekko.actor.typed.ActorSystem
import csw.location.api.models.{PekkoLocation, ComponentId}
import csw.prefix.models.Prefix
import esw.agent.pekko.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models.*
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}

/**
 * Pekko actor client for the Agent Service
 *
 * @param locationServiceUtil - an instance of locationServiceUtil
 * @param agentStatusUtil - an instance of agentStatusUtil
 * @param actorSystem - an implicit Pekko ActorSystem
 */
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
      .mapToAdt(identity, Failed.apply)

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] =
    agentClient(agentPrefix)
      .flatMapRight(_.spawnSequenceComponent(componentName, version))
      .mapToAdt(identity, Failed.apply)

  override def spawnContainers(
      agentPrefix: Prefix,
      hostConfigPath: String,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] =
    agentClient(agentPrefix).flatMapRight(_.spawnContainers(hostConfigPath, isConfigLocal)).mapToAdt(identity, Failed.apply)

  override def killComponent(componentId: ComponentId): Future[KillResponse] = {
    val compLocationE = locationServiceUtil.findPekkoLocation(componentId.prefix.toString(), componentId.componentType)
    compLocationE
      .flatMapE { compLocation =>
        val agentLocation = locationServiceUtil.findAgentByHostname(compLocation.uri.getHost)
        val agentClient   = agentLocation.mapRight(makeAgentClient)
        agentClient.flatMapRight(_.killComponent(compLocation))
      }
      .mapToAdt(identity, e => Failed(e.msg))
  }

  override def getAgentStatus: Future[AgentStatusResponse] = agentStatusUtil.getAllAgentStatus

  private[impl] def agentClient(agentPrefix: Prefix): Future[Either[String, AgentClient]] =
    AgentClient.make(agentPrefix, locationServiceUtil).mapLeft(e => e.msg)

  private[impl] def makeAgentClient(pekkoLocation: PekkoLocation): AgentClient = new AgentClient(pekkoLocation)
}
