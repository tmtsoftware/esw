package esw.http.core.utils

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.ICommandServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class ComponentFactory(locationService: LocationService, commandServiceFactory: ICommandServiceFactory)(
    implicit typedSystem: ActorSystem[_]
) {
  import typedSystem.executionContext

  private[http] def resolve[T](componentId: ComponentId)(f: AkkaLocation => T): Future[T] =
    locationService
      .resolve(AkkaConnection(componentId), 5.seconds)
      .map {
        case Some(akkaLocation) => f(akkaLocation)
        case None =>
          throw new IllegalArgumentException(
            s"Could not find component - ${componentId.name} of type - ${componentId.componentType}"
          )
      }

  def commandService(componentId: ComponentId): Future[CommandService] =
    resolve(componentId)(commandServiceFactory.make)
}
