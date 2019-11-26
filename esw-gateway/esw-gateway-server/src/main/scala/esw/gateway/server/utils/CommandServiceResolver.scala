package esw.gateway.server.utils

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class CommandServiceResolver(locationService: LocationService)(implicit typedSystem: ActorSystem[_]) {

  import typedSystem.executionContext

  def resolve(componentId: ComponentId): Future[Option[CommandService]] = {
    locationService.resolve(AkkaConnection(componentId), 5.seconds).map(_.map(CommandServiceFactory.make))
  }
}
