package esw.gateway.server.utils

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{ComponentId, Location}
import csw.location.api.scaladsl.LocationService
import esw.constants.CommonTimeouts

import scala.concurrent.Future

class ComponentFactory(
    locationService: LocationService,
    commandServiceFactory: ICommandServiceFactory = ICommandServiceFactory.default // todo: remove this and add function here
)(implicit typedSystem: ActorSystem[_]) {

  import typedSystem.executionContext

  private[esw] def resolveLocation[T](componentId: ComponentId)(f: Location => T): Future[T] =
    locationService
      .resolve(AkkaConnection(componentId), CommonTimeouts.ResolveLocation)
      .flatMap {
        case Some(akkaLocation) => Future.successful(akkaLocation)
        case None =>
          locationService
            .resolve(HttpConnection(componentId), CommonTimeouts.ResolveLocation)
            .map(_.getOrElse(throw ComponentNotFoundException(componentId)))
      }
      .map(f)

  def commandService(componentId: ComponentId): Future[CommandService] =
    resolveLocation(componentId)(commandServiceFactory.make)
}
