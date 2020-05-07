package esw.ocs.testkit

import java.nio.file.{Path, Paths}

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.testkit.FrameworkTestKit
import esw.agent.app.AgentSettings
import esw.ocs.testkit.utils.BaseTestSuite

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Random

trait TestKitWiring extends LocationUtils with BaseTestSuite {
  def underlyingFrameworkTestKit: FrameworkTestKit

  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = underlyingFrameworkTestKit.actorSystem

  implicit lazy val ec: ExecutionContext = underlyingFrameworkTestKit.frameworkWiring.actorRuntime.ec
  implicit lazy val askTimeout: Timeout  = Timeout(10.seconds)

  lazy val locationService: LocationService = underlyingFrameworkTestKit.frameworkWiring.locationService
  lazy val eventService: EventService       = underlyingFrameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher   = eventService.defaultPublisher
  lazy val gatewayTestKit                   = new GatewayTestKit(locationService)

  lazy val agentSettings: AgentSettings = AgentSettings(
    Paths.get(getClass.getResource("/").getPath).toString,
    durationToWaitForComponentRegistration = 5.seconds,
    durationToWaitForGracefulProcessTermination = 2.seconds
  )
  lazy val agentPrefix: Prefix = Prefix(s"esw.machine_${Random.nextInt().abs}")
}
