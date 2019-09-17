package esw.highlevel.dsl

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

// fixme: delete this, duplicate of one present in http-core
trait ComponentFactory {

  private[esw] def _locationService: LocationService
  private[esw] def actorSystem: ActorSystem[_]

  private[dsl] def resolve[T](componentName: String, componentType: ComponentType)(f: AkkaLocation => T): Future[T] =
    _locationService
      .resolve(AkkaConnection(ComponentId(componentName, componentType)), 5.seconds)
      .map {
        case Some(akkaLocation) => f(akkaLocation)
        case None               => throw new IllegalArgumentException(s"Could not find component - $componentName of type - $componentType")
      }(actorSystem.executionContext)

  def commandService(componentName: String, componentType: ComponentType): Future[CommandService] =
    resolve(componentName, componentType)(CommandServiceFactory.make(_)(actorSystem))
}
