package esw.gateway.server.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.Connection.AkkaConnection
import esw.gateway.api.protocol.InvalidComponent
import esw.ocs.api.SequencerApi
import esw.ocs.impl.SequencerActorProxy

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class Resolver(locationService: LocationService)(implicit typedSystem: ActorSystem[_]) {

  import typedSystem.executionContext
  private implicit val timeout: Timeout = 5.seconds

  def resolveComponent(componentId: ComponentId): Future[CommandService] =
    locationService
      .resolve(AkkaConnection(componentId), timeout.duration)
      .map(_.getOrElse(throw InvalidComponent(s"No component is registered with id $componentId")))
      .map(CommandServiceFactory.make)

  def resolveSequencer(componentId: ComponentId): Future[SequencerApi] =
    locationService
      .resolve(AkkaConnection(componentId), timeout.duration)
      .map(_.getOrElse(throw InvalidComponent(s"No sequencer is registered with id $componentId")))
      .map(loc => new SequencerActorProxy(loc.sequencerRef))
}
