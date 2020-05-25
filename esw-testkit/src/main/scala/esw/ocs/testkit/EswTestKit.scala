package esw.ocs.testkit

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.FrameworkTestKit
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.testkit.Service.{AAS, Gateway, MachineAgent}

abstract class EswTestKit(services: Service*)
    extends ScalaTestFrameworkTestKit(Service.convertToCsw(services): _*)
    with TestKitWiring {

  def underlyingFrameworkTestKit: FrameworkTestKit = frameworkTestKit

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (services.contains(AAS)) startKeycloak()
    if (services.contains(Gateway)) spawnGateway()
    if (services.contains(MachineAgent)) spawnAgent(agentSettings)
  }

  override def afterAll(): Unit = {
    shutdownAllSequencers()
    shutdownGateway()
    shutdownAgent()
    stopKeycloak()
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
