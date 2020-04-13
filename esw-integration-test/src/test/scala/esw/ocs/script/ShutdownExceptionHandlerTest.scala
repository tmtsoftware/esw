package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.TCS
import esw.ocs.api.protocol.Ok
import esw.ocs.api.actor.messages.SequencerMessages.Shutdown
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.EventServer

class ShutdownExceptionHandlerTest extends EswTestKit(EventServer) {

  private val tcsSubsystem     = TCS
  private val tcsObservingMode = "exceptionscript3" // ExceptionTestScript3.kt

  override def afterAll(): Unit = {
    clearAll()
    super.afterAll()
  }

  "should not invoke exception handler when handle-shutdown-failed" in {
    val reason            = "handle-shutdown-failed"
    val eventKey          = EventKey(Prefix("tcs.filter.wheel"), EventName(reason))
    val subscriptionProbe = TestProbe[Event]
    val subscription      = eventSubscriber.subscribeActorRef(Set(eventKey), subscriptionProbe.ref)
    subscription.ready().futureValue
    subscriptionProbe.expectMessageType[SystemEvent] // discard msg

    val sequencer = spawnSequencerRef(tcsSubsystem, tcsObservingMode)

    val shutdownProbe = TestProbe[Ok.type]
    sequencer ! Shutdown(shutdownProbe.ref)

    eventually { shutdownProbe.expectMessage(Ok) }
    eventually {
      // exception in shutdown handler will not call onGlobalError
      // So subscription probe won't receive msg
      subscriptionProbe.expectNoMessage()
    }
  }
}
