package esw.gateway.server.utils

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId}
import esw.gateway.api.CommandServiceFactoryApi
import esw.gateway.api.protocol.InvalidComponent

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class CommandServiceFactory(locationService: LocationService)(implicit typedSystem: ActorSystem[_])
    extends CommandServiceFactoryApi {

  import typedSystem.executionContext

  private def resolve[T](componentId: ComponentId)(f: AkkaLocation => T): Future[Either[InvalidComponent, T]] =
    locationService
      .resolve(AkkaConnection(componentId), 5.seconds)
      .map {
        case Some(akkaLocation) => Right(f(akkaLocation))
        case None =>
          Left(InvalidComponent(s"Could not find component - ${componentId.name} of type - ${componentId.componentType}"))
      }

  def commandService(componentId: ComponentId): Future[Either[InvalidComponent, CommandService]] =
    resolve(componentId)(client.CommandServiceFactory.make)
}
