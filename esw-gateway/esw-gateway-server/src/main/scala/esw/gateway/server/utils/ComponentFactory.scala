package esw.gateway.server.utils

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{ComponentId, Location}
import csw.location.api.scaladsl.LocationService

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class ComponentFactory(
    locationService: LocationService,
    commandServiceFactory: ICommandServiceFactory = ICommandServiceFactory.default
)(implicit typedSystem: ActorSystem[_]) {

  import typedSystem.executionContext
  private val timeout = 5.seconds

  private[esw] def resolveLocation[T](componentId: ComponentId)(f: Location => T): Future[T] =
    locationService
      .resolve(AkkaConnection(componentId), timeout)
      .flatMap {
        case Some(akkaLocation) => Future.successful(akkaLocation)
        case None =>
          locationService
            .resolve(HttpConnection(componentId), timeout)
            .map(_.getOrElse(throw ComponentNotFoundException(componentId)))
      }
      .map(f)

  def commandService(componentId: ComponentId): Future[CommandService] =
    resolveLocation(componentId)(commandServiceFactory.make)
}
