package esw.template.http.server

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.models.{AkkaLocation, ComponentType}

import scala.concurrent.Future

class ComponentFactory(locationService: LocationServiceWrapper)(implicit typedSystem: ActorSystem[_]) {

  def assemblyCommandService(assemblyName: String): Future[CommandService] = {
    locationService.resolve(assemblyName, ComponentType.Assembly)(akkaLocation => CommandServiceFactory.make(akkaLocation))
  }

  def hcdCommandService(hcdName: String): Future[CommandService] = {
    locationService.resolve(hcdName, ComponentType.HCD)(akkaLocation => CommandServiceFactory.make(akkaLocation))
  }

  def assemblyLocation(assemblyName: String): Future[AkkaLocation] = {
    locationService.resolve(assemblyName, ComponentType.Assembly)(identity)
  }
}
