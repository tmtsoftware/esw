package esw.template.http.server

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.ICommandServiceFactory
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class ComponentFactory(locationService: LocationService, commandServiceFactory: ICommandServiceFactory)(
    implicit typedSystem: ActorSystem[_],
    ec: ExecutionContext
) {

  private[server] def resolve[T](componentName: String, componentType: ComponentType)(f: AkkaLocation => T): Future[T] =
    locationService
      .resolve(AkkaConnection(ComponentId(componentName, componentType)), 5.seconds)
      .map {
        case Some(akkaLocation) =>
          f(akkaLocation)
        case None =>
          throw new IllegalArgumentException(s"Could not find component - $componentName of type - $componentType")
      }

  def assemblyCommandService(assemblyName: String): Future[CommandService] = {
    resolve(assemblyName, ComponentType.Assembly)(akkaLocation => {
      commandServiceFactory.make(akkaLocation)
    })
  }

  def hcdCommandService(hcdName: String): Future[CommandService] = {
    resolve(hcdName, ComponentType.HCD)(akkaLocation => {
      commandServiceFactory.make(akkaLocation)
    })
  }

  def commandService(componentName: String, componentType: ComponentType): Future[CommandService] = componentType match {
    case Assembly => assemblyCommandService(componentName)
    case HCD      => hcdCommandService(componentName)
  }
}
