package esw.agent.client

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Machine
import csw.location.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.{AgentCommand, KillResponse, SpawnResponse}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class AgentClient private[agent] (agentRef: ActorRef[AgentCommand])(implicit scheduler: Scheduler) {
  implicit private val timeout: Timeout = Timeout(10.seconds)

  def spawnSequenceComponent(prefix: Prefix): Future[SpawnResponse] =
    agentRef ? (SpawnSequenceComponent(_, prefix))

  def killComponent(componentId: ComponentId): Future[KillResponse] =
    agentRef ? (KillComponent(_, componentId))
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
