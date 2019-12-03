package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequence
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands._
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol._
import esw.ocs.impl.SequencerActorProxy
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.testkit.EswTestKit
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2

class ExceptionsHandlerIntegrationTest extends EswTestKit(EventServer) {
  private val ocsPackageId     = "esw"
  private val ocsObservingMode = "exceptionscript" // ExceptionTestScript.kt

  private val tcsPackageId     = "tcs"
  private val tcsObservingMode = "exceptionscript2" // ExceptionTestScript2.kt

  override def afterEach(): Unit = shutdownAllSequencers()

  "Script" must {

    // *********  Test cases of Idle state *************
    val setupSequence   = Sequence(Setup(Prefix("TCS"), CommandName("fail-setup"), None))
    val observeSequence = Sequence(Observe(Prefix("TCS"), CommandName("fail-observe"), None))

    val idleStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
      ("sequencer msg", "failure msg"),
      (SubmitSequence(setupSequence, TestProbe[SubmitResponse].ref), "handle-setup-failed"),
      (SubmitSequence(observeSequence, TestProbe[SubmitResponse].ref), "handle-observe-failed"),
      (GoOffline(TestProbe[GoOfflineResponse].ref), "handle-goOffline-failed"),
      (OperationsMode(TestProbe[OperationsModeResponse].ref), "handle-operations-mode-failed"),
      (DiagnosticMode(UTCTime.now(), "any", TestProbe[DiagnosticModeResponse].ref), "handle-diagnostic-mode-failed")
    )

    forAll(idleStateTestCases) { (msg, failureMessage) =>
      s"invoke exception handler when $failureMessage | ESW-139" in {
        val sequencer = spawnSequencerRef(ocsPackageId, ocsObservingMode)

        val eventKey = EventKey("tcs." + failureMessage)
        val probe    = createProbeFor(eventKey)

        sequencer ! msg

        assertMessage(probe, failureMessage)
      }
    }

//    ********* Test cases of InProgress state *************
    val inProgressStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
      ("sequencer msg", "failure msg"),
      (Stop(TestProbe[OkOrUnhandledResponse].ref), "handle-stop-failed"),
      (AbortSequence(TestProbe[OkOrUnhandledResponse].ref), "handle-abort-failed")
    )

    forAll(inProgressStateTestCases) { (msg, failureMessage) =>
      s"invoke exception handler when $failureMessage | ESW-139" in {
        val sequencerRef = spawnSequencerRef(ocsPackageId, ocsObservingMode)
        val sequencer    = new SequencerActorProxy(sequencerRef)

        val eventKey = EventKey("tcs." + failureMessage)
        val probe    = createProbeFor(eventKey)

        val longRunningSetupCommand  = Setup(Prefix("TCS"), CommandName("long-running-setup"), None)
        val command1                 = Setup(Prefix("TCS"), CommandName("successful-command"), None)
        val longRunningSetupSequence = Sequence(longRunningSetupCommand, command1)

        sequencer.submit(longRunningSetupSequence)
        // Pause sequence so it will remain in InProgress state and then other inProgressState msgs can be processed
        sequencer.pause
        sequencerRef ! msg

        assertMessage(probe, failureMessage)
      }
    }
  }

  "Script2" must {

    "invoke exception handlers when exception is thrown from handler and must fail the command with message of given exception | ESW-139" in {
      val sequencerRef = spawnSequencerRef(ocsPackageId, ocsObservingMode)
      val sequencer    = new SequencerActorProxy(sequencerRef)

      val command  = Setup(Prefix("TCS"), CommandName("fail-setup"), None)
      val sequence = Sequence(Seq(command))

      val commandFailureMsg = "handle-setup-failed"
      val eventKey          = EventKey("tcs." + commandFailureMsg)

      val testProbe = createProbeFor(eventKey)

      val submitResponseF = sequencer.submitAndWait(sequence)
      val error           = submitResponseF.futureValue.asInstanceOf[CommandResponse.Error]
      error.message.contains(commandFailureMsg) shouldBe true

      // exception handler publishes a event with exception msg as event name
      val event = testProbe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe commandFailureMsg

      // assert that next sequence is accepted and executed properly
      val command1  = Setup(Prefix("TCS"), CommandName("successful-command"), None)
      val sequence1 = Sequence(Seq(command1))

      sequencer.submitAndWait(sequence1).futureValue shouldBe a[Completed]
    }

    "invoke exception handler when handle-goOnline-failed | ESW-139" in {
      val globalExHandlerEventMessage = "handle-goOnline-failed"
      val eventKey                    = EventKey("tcs." + globalExHandlerEventMessage)
      val testProbe                   = createProbeFor(eventKey)

      val sequencerRef = spawnSequencerRef(tcsPackageId, tcsObservingMode)
      val sequencer    = new SequencerActorProxy(sequencerRef)

      sequencer.goOffline().awaitResult
      sequencer.goOnline()

      assertMessage(testProbe, globalExHandlerEventMessage)
    }

    "call global exception handler if there is an exception in command handler even after retrying | ESW-249" in {
      val globalExHandlerEventMessage   = "command-failed"
      val globalExHandlerEventKey       = EventKey("tcs." + globalExHandlerEventMessage)
      val globalExHandlerEventTestProbe = createProbeFor(globalExHandlerEventKey)

      val onErrorEventMessage   = "onError-event"
      val onErrorEventKey       = EventKey("tcs." + onErrorEventMessage)
      val onErrorEventTestProbe = createProbeFor(onErrorEventKey)

      val sequencerRef  = spawnSequencerRef(tcsPackageId, tcsObservingMode)
      val sequencer     = new SequencerActorProxy(sequencerRef)
      val setupSequence = Sequence(Setup(Prefix("TCS"), CommandName("error-handling"), None))

      sequencer.submitAndWait(setupSequence)

      assertMessage(onErrorEventTestProbe, onErrorEventMessage)
      assertMessage(onErrorEventTestProbe, onErrorEventMessage)
      assertMessage(onErrorEventTestProbe, onErrorEventMessage)

      assertMessage(globalExHandlerEventTestProbe, globalExHandlerEventMessage)
    }
  }

  private def assertMessage(probe: TestProbe[Event], reason: String): Unit = {
    eventually {
      val event = probe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe reason
    }
  }

  private def createProbeFor(eventKey: EventKey): TestProbe[Event] = {
    val testProbe    = TestProbe[Event]
    val subscription = eventSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
    subscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard msg
    testProbe
  }
}
