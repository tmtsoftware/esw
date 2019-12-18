package esw.gateway.server.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{ComponentId, Location}
import esw.gateway.api.protocol.InvalidComponent
import esw.ocs.api.SequencerApi
import esw.ocs.impl.SequencerApiFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class Resolver(locationService: LocationService)(implicit typedSystem: ActorSystem[_]) {

  import typedSystem.executionContext
  private implicit val timeout: Timeout = 5.seconds

  private def resolveLocation(componentId: ComponentId): Future[Location] =
    locationService
      .resolve(AkkaConnection(componentId), timeout.duration)
      .flatMap {
        case Some(akkaLocation) => Future.successful(akkaLocation)
        case None =>
          locationService
            .resolve(HttpConnection(componentId), timeout.duration)
            .map(_.getOrElse(throw InvalidComponent(s"No component is registered with id $componentId")))
      }

  def resolveComponent(componentId: ComponentId): Future[CommandService] =
    resolveLocation(componentId).map(CommandServiceFactory.make)

  def resolveSequencer(componentId: ComponentId): Future[SequencerApi] =
    resolveLocation(componentId).map(SequencerApiFactory.make)
}
