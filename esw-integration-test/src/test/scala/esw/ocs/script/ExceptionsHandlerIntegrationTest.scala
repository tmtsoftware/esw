package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequence
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandResponse, _}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Subsystem.{ESW, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.time.core.models.UTCTime
import esw.ocs.api.actor.client.SequencerImpl
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol._
import esw.ocs.testkit.EswTestKit
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2

class ExceptionsHandlerIntegrationTest extends EswTestKit(EventServer) {
  private val ocsSubsystem = ESW
  private val ocsObsMode   = ObsMode("exceptionscript") // ExceptionTestScript.kt

  private val tcsSubsystem = TCS
  private val tcsObsMode   = ObsMode("exceptionscript2") // ExceptionTestScript2.kt

  private val prefix = Prefix("tcs.filter.wheel")

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standaloneHcd.conf"))
  }

  override def afterEach(): Unit = shutdownAllSequencers()

  "Script" must {

    // *********  Test cases of Idle state *************
    val setupSequence   = Sequence(Setup(Prefix("TCS.test"), CommandName("fail-setup"), None))
    val observeSequence = Sequence(Observe(Prefix("TCS.test"), CommandName("fail-observe"), None))

    val idleStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
      ("sequencer msg", "failure msg"),
      (SubmitSequence(setupSequence, TestProbe[SubmitResponse]().ref), "handle-setup-failed"),
      (SubmitSequence(observeSequence, TestProbe[SubmitResponse]().ref), "handle-observe-failed"),
      (GoOffline(TestProbe[GoOfflineResponse]().ref), "handle-goOffline-failed"),
      (OperationsMode(TestProbe[OperationsModeResponse]().ref), "handle-operations-mode-failed"),
      (DiagnosticMode(UTCTime.now(), "any", TestProbe[DiagnosticModeResponse]().ref), "handle-diagnostic-mode-failed")
    )

    forAll(idleStateTestCases) { (msg, failureMessage) =>
      s"invoke exception handler when $failureMessage | ESW-139, CSW-81" in {
        val sequencer = spawnSequencerRef(ocsSubsystem, ocsObsMode)

        val eventKey = EventKey(prefix, EventName(failureMessage))
        val probe    = createProbeFor(eventKey)

        sequencer ! msg

        assertMessage(probe, failureMessage)
      }
    }

//    ********* Test cases of InProgress state *************
    val inProgressStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
      ("sequencer msg", "failure msg"),
      (Stop(TestProbe[OkOrUnhandledResponse]().ref), "handle-stop-failed"),
      (AbortSequence(TestProbe[OkOrUnhandledResponse]().ref), "handle-abort-failed")
    )

    forAll(inProgressStateTestCases) { (msg, failureMessage) =>
      s"invoke exception handler when $failureMessage | ESW-139, CSW-81" in {
        val sequencerRef = spawnSequencerRef(ocsSubsystem, ocsObsMode)
        val sequencer    = new SequencerImpl(sequencerRef)

        val eventKey = EventKey(prefix, EventName(failureMessage))
        val probe    = createProbeFor(eventKey)

        val longRunningSetupCommand  = Setup(Prefix("TCS.test"), CommandName("long-running-setup"), None)
        val command1                 = Setup(Prefix("TCS.test"), CommandName("successful-command"), None)
        val longRunningSetupSequence = Sequence(longRunningSetupCommand, command1)

        sequencer.submit(longRunningSetupSequence)
        Thread.sleep(500)
        // Pause sequence, so it will remain in InProgress state and then other inProgressState msgs can be processed
        sequencer.pause
        sequencerRef ! msg

        assertMessage(probe, failureMessage)
      }
    }
  }

  "Script2" must {

    "invoke exception handlers when exception is thrown from handler and must fail the command with message of given exception | ESW-139, CSW-81" in {
      val sequencer = spawnSequencerProxy(ocsSubsystem, ocsObsMode)

      val command  = Setup(Prefix("TCS.test"), CommandName("fail-setup"), None)
      val sequence = Sequence(command)

      val commandFailureMsg = "handle-setup-failed"
      val eventKey          = EventKey(prefix, EventName(commandFailureMsg))

      val testProbe = createProbeFor(eventKey)

      val submitResponseF = sequencer.submitAndWait(sequence)
      val error           = submitResponseF.futureValue.asInstanceOf[CommandResponse.Error]
      error.message.contains(commandFailureMsg) shouldBe true

      // exception handler publishes a event with exception msg as event prefix
      val event = testProbe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe commandFailureMsg

      // assert that next sequence is accepted and executed properly
      val command1  = Setup(Prefix("TCS.test"), CommandName("successful-command"), None)
      val sequence1 = Sequence(command1)

      sequencer.submitAndWait(sequence1).futureValue shouldBe a[Completed]
    }

    //    ********* Test case of Offline state *************
    "invoke exception handler when handle-goOnline-failed | ESW-139, CSW-81" in {
      val globalExHandlerEventMessage = "handle-goOnline-failed"
      val eventKey                    = EventKey(prefix, EventName(globalExHandlerEventMessage))
      val testProbe                   = createProbeFor(eventKey)

      val sequencer = spawnSequencerProxy(tcsSubsystem, tcsObsMode)

      sequencer.goOffline().futureValue
      sequencer.goOnline()

      assertMessage(testProbe, globalExHandlerEventMessage)
    }

    "call global exception handler if there is an exception in command handler even after retrying | ESW-249, ESW-139, CSW-81, ESW-294" in {
      val globalExHandlerEventTestProbe = createProbeFromPSubscript(TCS, "filter.wheel.Error*")

      val onErrorEventMessage   = "onError-event"
      val onErrorEventKey       = EventKey(prefix, EventName(onErrorEventMessage))
      val onErrorEventTestProbe = createProbeFor(onErrorEventKey)

      val sequencer     = spawnSequencerProxy(tcsSubsystem, tcsObsMode)
      val failCmdName   = CommandName("error-handling")
      val setupSequence = Sequence(Setup(prefix, failCmdName, None))

      val responseF = sequencer.submitAndWait(setupSequence)

      assertMessage(onErrorEventTestProbe, onErrorEventMessage)
      assertMessage(onErrorEventTestProbe, onErrorEventMessage)
      assertMessage(onErrorEventTestProbe, onErrorEventMessage)

      // verify global handler
      val event = globalExHandlerEventTestProbe.expectMessageType[SystemEvent]
      event.isInvalid shouldBe false
      event.eventName.name should fullyMatch regex """Error\(Id\(.*\),command-failed\)"""

      //ESW-294
      val error = responseF.futureValue.asInstanceOf[CommandResponse.Error]
      error.message should fullyMatch regex s"""StepId: .*, CommandName: ${failCmdName.name}, reason: Error\\(Id\\(.*\\),command-failed\\)"""
    }

    "not fail the command on negative submit response if resumeOnError=false | ESW-249, ESW-139, CSW-81" in {
      val negativeSubmitResEventMessage   = "negative-response-error"
      val negativeSubmitResEventKey       = EventKey(prefix, EventName(negativeSubmitResEventMessage))
      val negativeSubmitResEventTestProbe = createProbeFor(negativeSubmitResEventKey)

      val sequencer     = spawnSequencerProxy(tcsSubsystem, tcsObsMode)
      val setupSequence = Sequence(Setup(prefix, CommandName("negative-submit-response"), None))

      val submitResponse = sequencer.submit(setupSequence).futureValue

      assertMessage(negativeSubmitResEventTestProbe, negativeSubmitResEventMessage)
      sequencer.queryFinal(submitResponse.runId).futureValue should ===(Completed(submitResponse.runId))
    }
  }

  private def assertMessage(probe: TestProbe[Event], reason: String): Unit = {
    eventually {
      val event = probe.expectMessageType[SystemEvent]
      event.isInvalid shouldBe false
      event.eventName.name shouldBe reason
    }
  }

  private def createProbeFor(eventKey: EventKey): TestProbe[Event] = {
    val testProbe    = TestProbe[Event]()
    val subscription = eventSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
    subscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard msg
    testProbe
  }

  private def createProbeFromPSubscript(subsystem: Subsystem, pattern: String): TestProbe[Event] = {
    val testProbe    = TestProbe[Event]()
    val subscription = eventSubscriber.pSubscribeCallback(subsystem, pattern, testProbe.ref ! _)
    subscription.ready().futureValue
    testProbe
  }
}
