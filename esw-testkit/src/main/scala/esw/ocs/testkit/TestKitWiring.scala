package esw.ocs.testkit

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.scaladsl.LocationService
import csw.testkit.FrameworkTestKit
import esw.ocs.testkit.utils.{AgentServiceUtils, AgentUtils, GatewayUtils, KeycloakUtils, LocationUtils, SequencerUtils}

trait TestKitWiring
    extends LocationUtils
    with SequencerUtils
    with AgentUtils
    with GatewayUtils
    with KeycloakUtils
    with AgentServiceUtils {
  def underlyingFrameworkTestKit: FrameworkTestKit

  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = underlyingFrameworkTestKit.actorSystem

  lazy val locationService: LocationService = underlyingFrameworkTestKit.frameworkWiring.locationService
  lazy val eventService: EventService       = underlyingFrameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher   = eventService.defaultPublisher
}
