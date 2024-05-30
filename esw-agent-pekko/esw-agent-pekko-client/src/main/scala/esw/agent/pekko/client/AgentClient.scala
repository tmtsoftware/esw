package esw.agent.pekko.client

import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, Location}
import csw.prefix.models.Prefix
import esw.agent.pekko.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.pekko.client.AgentCommand.{KillComponent, SpawnContainers}
import esw.agent.service.api.models.*
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.constants.AgentTimeouts

import java.nio.file.Path
import scala.concurrent.Future

/**
 * Pekko client for the Agent
 * @param pekkoLocation - [[csw.location.api.models.PekkoLocation]] of the Agent Server.
 * @param actorSystem - [[org.apache.pekko.actor.typed.ActorSystem]] - an Pekko ActorSystem.
 */
class AgentClient(pekkoLocation: PekkoLocation)(implicit actorSystem: ActorSystem[?]) {
  private val agentRef: ActorRef[AgentCommand] = pekkoLocation.uri.toActorRef.unsafeUpcast[AgentCommand]
  private val agentPrefix                      = pekkoLocation.prefix

  def spawnSequenceComponent(
      componentName: String,
      version: Option[String] = None,
      simulation: Boolean = false
  ): Future[SpawnResponse] =
    (agentRef ? (SpawnSequenceComponent(_: ActorRef[SpawnResponse], agentPrefix, componentName, version, simulation)))(
      AgentTimeouts.SpawnComponent,
      actorSystem.scheduler
    )

  def spawnSequenceManager(
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String] = None,
      simulation: Boolean = false
  ): Future[SpawnResponse] =
    (agentRef ? (SpawnSequenceManager(_: ActorRef[SpawnResponse], obsModeConfigPath, isConfigLocal, version, simulation)))(
      AgentTimeouts.SpawnComponent,
      actorSystem.scheduler
    )

  def spawnContainers(
      hostConfigPath: String,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] =
    (agentRef ? (SpawnContainers(_: ActorRef[SpawnContainersResponse], hostConfigPath, isConfigLocal)))(
      AgentTimeouts.SpawnComponent,
      actorSystem.scheduler
    )

  def killComponent(location: Location): Future[KillResponse] =
    (agentRef ? (KillComponent(_: ActorRef[KillResponse], location)))(AgentTimeouts.KillComponent, actorSystem.scheduler)
}

object AgentClient {

  // create AgentClient(Actor client or proxy) for the agent of the given prefix
  // if there is no agent running with the given prefix it returns the FindLocationError as a Future
  // else AgentClient gets returned as a Future
  def make(agentPrefix: Prefix, locationService: LocationServiceUtil)(implicit
      actorSystem: ActorSystem[?]
  ): Future[Either[EswLocationError.FindLocationError, AgentClient]] = {
    import actorSystem.executionContext
    locationService
      .find(PekkoConnection(ComponentId(agentPrefix, Machine)))
      .mapRight(new AgentClient(_))
  }
}
