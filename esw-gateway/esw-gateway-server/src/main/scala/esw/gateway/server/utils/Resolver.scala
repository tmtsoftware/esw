package esw.gateway.server.utils

import org.apache.pekko.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentId
import csw.location.api.scaladsl.LocationService
import esw.gateway.api.protocol.InvalidComponent
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory

import scala.concurrent.Future

/**
 * This class provides functionality to resolve hcd/assembly/container/sequencer using component factory.
 * @param locationService - an instance of locationService
 * @param typedSystem - an instance of pekko actor typed system
 */
class Resolver(locationService: LocationService)(implicit typedSystem: ActorSystem[?]) {
  import typedSystem.executionContext

  private val componentFactory = new ComponentFactory(locationService)

  def commandService(componentId: ComponentId): Future[CommandService] =
    componentFactory.commandService(componentId).recover { case e: ComponentNotFoundException =>
      throw InvalidComponent(e.getMessage)
    }

  def sequencerCommandService(componentId: ComponentId): Future[SequencerApi] =
    componentFactory.resolveLocation(componentId)(SequencerApiFactory.make).recover { case e: ComponentNotFoundException =>
      throw InvalidComponent(e.getMessage)
    }

}
