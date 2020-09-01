package esw.agent.akka.client

import java.nio.file.Path

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentCommand.KillComponent
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.constants.Timeouts

import scala.concurrent.Future
import scala.jdk.DurationConverters.JavaDurationOps

class AgentClient(akkaLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]) {
  private implicit val timeout: Timeout = actorSystem.settings.config.getDuration("agent.akka.client.askTimeout").toScala

  private val agentRef: ActorRef[AgentCommand] = akkaLocation.uri.toActorRef.unsafeUpcast[AgentCommand]
  private val agentPrefix                      = akkaLocation.prefix

  def spawnSequenceComponent(componentName: String, version: Option[String] = None): Future[SpawnResponse] =
    (agentRef ? (SpawnSequenceComponent(_, agentPrefix, componentName, version)))(Timeouts.AgentSpawn, actorSystem.scheduler)

  def spawnSequenceManager(
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String] = None
  ): Future[SpawnResponse] =
    (agentRef ? (SpawnSequenceManager(_, obsModeConfigPath, isConfigLocal, version)))(Timeouts.AgentSpawn, actorSystem.scheduler)

  def killComponent(location: Location): Future[KillResponse] = agentRef ? (KillComponent(_, location))
}

object AgentClient {
  def make(agentPrefix: Prefix, locationService: LocationServiceUtil)(implicit
      actorSystem: ActorSystem[_]
  ): Future[Either[EswLocationError.FindLocationError, AgentClient]] = {
    import actorSystem.executionContext
    locationService
      .find(AkkaConnection(ComponentId(agentPrefix, Machine)))
      .mapRight(new AgentClient(_))
  }
}
