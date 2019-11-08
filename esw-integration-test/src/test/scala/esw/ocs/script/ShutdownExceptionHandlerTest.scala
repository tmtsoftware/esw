package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.Ok
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.messages.SequencerMessages.Shutdown

import scala.concurrent.duration.DurationInt

class ShutdownExceptionHandlerTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  override implicit def patienceConfig: PatienceConfig         = PatienceConfig(10.seconds)

  private val tcsPackageId     = "tcs"
  private val tcsObservingMode = "exceptionscript3" // ExceptionTestScript3.kt

  private val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)

  "invoke exception handler when handle-shutdown-failed" in {
    val reason         = "handle-shutdown-failed"
    val eventKey       = EventKey("tcs." + reason)
    val assertionProbe = TestProbe[Event]
    val subscription   = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), assertionProbe.ref)
    subscription.ready().futureValue
    assertionProbe.expectMessageType[SystemEvent] // discard msg

    val wiring    = new SequencerWiring(tcsPackageId, tcsObservingMode, None)
    val sequencer = wiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]

    val shutdownProbe = TestProbe[Ok.type]
    sequencer ! Shutdown(shutdownProbe.ref)

    eventually {
      val event = assertionProbe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe reason
      shutdownProbe.expectMessage(Ok)
    }
  }
}
