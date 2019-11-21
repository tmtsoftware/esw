package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.protocol.Ok
import esw.ocs.impl.messages.SequencerMessages.Shutdown
import esw.ocs.testkit.EswTestKit

class ShutdownExceptionHandlerTest extends EswTestKit(EventServer) {

  private val tcsPackageId     = "tcs"
  private val tcsObservingMode = "exceptionscript3" // ExceptionTestScript3.kt

  override def afterAll(): Unit = {
    clearAllWirings()
    super.afterAll()
  }

  "invoke exception handler when handle-shutdown-failed" in {
    val reason         = "handle-shutdown-failed"
    val eventKey       = EventKey("tcs." + reason)
    val assertionProbe = TestProbe[Event]
    val subscription   = eventSubscriber.subscribeActorRef(Set(eventKey), assertionProbe.ref)
    subscription.ready().futureValue
    assertionProbe.expectMessageType[SystemEvent] // discard msg

    val sequencer = spawnSequencerRef(tcsPackageId, tcsObservingMode)

    val shutdownProbe = TestProbe[Ok.type]
    sequencer ! Shutdown(shutdownProbe.ref)

    eventually {
      val event = assertionProbe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe reason
      shutdownProbe.expectMessage(Ok)
    }
  }
}
