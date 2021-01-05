package esw.ocs.testkit

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.framework.internal.wiring.FrameworkWiring
import csw.framework.testkit.SpawnComponent
import csw.location.api.scaladsl.LocationService
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.FrameworkTestKit
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.testkit.Service.{AAS, AgentService, Gateway, MachineAgent}
import esw.ocs.testkit.utils._

abstract class EswTestKit(services: Service*)
    extends ScalaTestFrameworkTestKit(Service.convertToCsw(services): _*)
    with LocationUtils
    with SpawnComponent
    with SequencerUtils
    with AgentUtils
    with GatewayUtils
    with KeycloakUtils
    with AgentServiceUtils {

  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = underlyingFrameworkTestKit.actorSystem

  override lazy val wiring: FrameworkWiring = frameworkTestKit.frameworkWiring

  lazy val locationService: LocationService = underlyingFrameworkTestKit.frameworkWiring.locationService
  lazy val eventService: EventService       = underlyingFrameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher   = eventService.defaultPublisher

  def underlyingFrameworkTestKit: FrameworkTestKit = frameworkTestKit

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (services.contains(AAS)) startKeycloak()
    if (services.contains(Gateway)) spawnGateway()
    if (services.contains(MachineAgent)) spawnAgent(agentSettings)
    if (services.contains(AgentService)) spawnAgentService()
  }

  override def afterAll(): Unit = {
    shutdownAllSequencers()
    shutdownGateway()
    shutdownAgent()
    stopKeycloak()
    shutdownAgentService()
    super.afterAll()
  }

  def createTestProbe(eventKeys: Set[EventKey]): TestProbe[Event] = {
    val testProbe    = TestProbe[Event]()
    val subscription = eventSubscriber.subscribeActorRef(eventKeys, testProbe.ref)
    subscription.ready().futureValue
    eventKeys.foreach(_ => testProbe.expectMessageType[SystemEvent]) // discard invalid event
    testProbe
  }
}
