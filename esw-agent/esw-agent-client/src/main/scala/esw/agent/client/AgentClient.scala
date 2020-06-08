package esw.agent.client

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.AgentCommand.{GetAgentStatus, GetComponentStatus, KillComponent}
import esw.agent.api._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class AgentClient private[agent] (agentRef: ActorRef[AgentCommand])(implicit scheduler: Scheduler) {
  implicit private val timeout: Timeout = Timeout(1.minute)

  def spawnSequenceComponent(prefix: Prefix, version: Option[String] = None): Future[SpawnResponse] =
    agentRef ? (SpawnSequenceComponent(_, prefix, version))

  def spawnRedis(prefix: Prefix, port: Int, redisArguments: List[String]): Future[SpawnResponse] =
    agentRef ? (SpawnRedis(_, prefix, port, redisArguments))

  def killComponent(componentId: ComponentId): Future[KillResponse] =
    agentRef ? (KillComponent(_, componentId))

  def getComponentStatus(componentId: ComponentId): Future[ComponentStatus] =
    agentRef ? (GetComponentStatus(_, componentId))

  def getAgentStatus: Future[AgentStatus] = agentRef ? GetAgentStatus
}

object AgentClient {
  def make(agentPrefix: Prefix, locationService: LocationService)(implicit actorSystem: ActorSystem[_]): Future[AgentClient] = {
    import actorSystem.executionContext
    implicit val sch: Scheduler   = actorSystem.scheduler
    val eventualMaybeAkkaLocation = locationService.resolve(AkkaConnection(ComponentId(agentPrefix, Machine)), 5.seconds)
    eventualMaybeAkkaLocation
      .map(_.getOrElse(throw new RuntimeException(s"could not resolve agent with prefix: $agentPrefix")))
      .map(_.uri.toActorRef.unsafeUpcast[AgentCommand])
      .map(new AgentClient(_))
  }
}
