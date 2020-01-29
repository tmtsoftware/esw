package esw.agent.client

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.{GetAgentStatus, GetComponentStatus, KillComponent}
import esw.agent.api.AgentCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.{AgentCommand, AgentStatus, ComponentStatus, KillResponse, SpawnResponse}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class AgentClient private[agent] (agentRef: ActorRef[AgentCommand])(implicit scheduler: Scheduler) {
  implicit private val timeout: Timeout = Timeout(10.seconds)

  def spawnSequenceComponent(prefix: Prefix): Future[SpawnResponse] =
    agentRef ? (SpawnSequenceComponent(_, prefix))

  def spawnRedis(prefix: Prefix, port: Int, redisArguments: List[String]): Future[SpawnResponse] =
    agentRef ? (SpawnRedis(_, prefix, port, redisArguments))

  def killComponent(componentId: ComponentId): Future[KillResponse] =
    agentRef ? (KillComponent(_, componentId))

  def getComponentStatus(componentId: ComponentId): Future[ComponentStatus] =
    agentRef ? (GetComponentStatus(_, componentId))

  def getAgentStatus: Future[AgentStatus] = agentRef ? GetAgentStatus
}
object AgentClient {
  def make(prefix: Prefix, locationService: LocationService)(implicit actorSystem: ActorSystem[_]): Future[AgentClient] = {
    import actorSystem.executionContext
    implicit val sch: Scheduler   = actorSystem.scheduler
    val eventualMaybeAkkaLocation = locationService.resolve(AkkaConnection(ComponentId(prefix, Machine)), 5.seconds)
    eventualMaybeAkkaLocation
      .map(_.getOrElse(throw new RuntimeException(s"could not resolve $prefix")))
      .map(_.uri.toActorRef.unsafeUpcast[AgentCommand])
      .map(new AgentClient(_))
  }
}
