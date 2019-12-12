package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.core.models.Prefix
import csw.params.core.models.Subsystem.TCS
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import esw.ocs.api.protocol.Ok
import esw.ocs.impl.messages.SequencerMessages.Shutdown
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.EventServer

class ShutdownExceptionHandlerTest extends EswTestKit(EventServer) {

  private val tcsSubsystem     = TCS
  private val tcsObservingMode = "exceptionscript3" // ExceptionTestScript3.kt

  override def afterAll(): Unit = {
    clearAll()
    super.afterAll()
  }

  "invoke exception handler when handle-shutdown-failed" in {
    val reason         = "handle-shutdown-failed"
    val eventKey       = EventKey(Prefix("tcs.filter.wheel"), EventName(reason))
    val assertionProbe = TestProbe[Event]
    val subscription   = eventSubscriber.subscribeActorRef(Set(eventKey), assertionProbe.ref)
    subscription.ready().futureValue
    assertionProbe.expectMessageType[SystemEvent] // discard msg

    val sequencer = spawnSequencerRef(tcsSubsystem, tcsObservingMode)

    val shutdownProbe = TestProbe[Ok.type]
    sequencer ! Shutdown(shutdownProbe.ref)

    eventually { shutdownProbe.expectMessage(Ok) }
    eventually {
      val event = assertionProbe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe reason
    }
  }
}
