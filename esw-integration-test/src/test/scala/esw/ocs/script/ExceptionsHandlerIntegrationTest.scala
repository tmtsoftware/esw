package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequenceAndWait
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands._
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol._
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.Timeouts
import esw.ocs.impl.messages.SequencerMessages._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ExceptionsHandlerIntegrationTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  private implicit val askTimeout: Timeout                     = Timeouts.DefaultTimeout
  override implicit def patienceConfig: PatienceConfig         = PatienceConfig(10.seconds)

  private val ocsPackageId     = "esw"
  private val ocsObservingMode = "exceptionscript" // ExceptionTestScript.kt

  private val tcsPackageId      = "tcs"
  private val tcsObservingMode  = "exceptionscript2" // ExceptionTestScript2.kt
  private val tcsObservingMode2 = "exceptionscript3" // ExceptionTestScript3.kt

  private val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)

  "Script" must {

    // *********  Test cases of Idle state *************
    val setupSequence   = Sequence(Setup(Prefix("TCS"), CommandName("fail-setup"), None))
    val observeSequence = Sequence(Observe(Prefix("TCS"), CommandName("fail-observe"), None))

    val idleStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
      ("sequencer msg", "failure msg"),
      (SubmitSequenceAndWait(setupSequence, TestProbe[SubmitResponse].ref), "handle-setup-failed"),
      (SubmitSequenceAndWait(observeSequence, TestProbe[SubmitResponse].ref), "handle-observe-failed"),
      (GoOffline(TestProbe[GoOfflineResponse].ref), "handle-goOffline-failed"),
      (OperationsMode(TestProbe[OperationsModeResponse].ref), "handle-operations-mode-failed"),
      (DiagnosticMode(UTCTime.now(), "any", TestProbe[DiagnosticModeResponse].ref), "handle-diagnostic-mode-failed")
    )

    forAll(idleStateTestCases) { (msg, reason) =>
      s"invoke exception handler when ${reason} | ESW-139" in {
        val setup = new SequencerSetup(ocsPackageId, ocsObservingMode)

        val eventKey = EventKey("tcs." + reason)
        val probe    = createProbeFor(eventKey)

        setup.sequencer ! msg

        assertReason(probe, reason)
        setup.shutdownSequencer()
      }
    }

//    ********* Test cases of InProgress state *************
    val inProgressStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
      ("sequencer msg", "failure msg"),
      (Stop(TestProbe[OkOrUnhandledResponse].ref), "handle-stop-failed"),
      (AbortSequence(TestProbe[OkOrUnhandledResponse].ref), "handle-abort-failed")
    )

    forAll(inProgressStateTestCases) { (msg, reason) =>
      s"invoke exception handler when ${reason} | ESW-139" in {
        val setup = new SequencerSetup(ocsPackageId, ocsObservingMode)

        val eventKey = EventKey("tcs." + reason)
        val probe    = createProbeFor(eventKey)

        val longRunningSetupCommand  = Setup(Prefix("TCS"), CommandName("long-running-setup"), None)
        val longRunningSetupSequence = Sequence(longRunningSetupCommand)

        setup.sequencer ? ((x: ActorRef[SubmitResponse]) => SubmitSequenceAndWait(longRunningSetupSequence, x))
        setup.sequencer ! msg

        assertReason(probe, reason)
        setup.shutdownSequencer()
      }
    }
  }

  "Script2" must {

    "invoke exception handlers when exception is thrown from handler and must fail the command with message of given exception | ESW-139" in {
      val setup = new SequencerSetup(ocsPackageId, ocsObservingMode)

      val command  = Setup(Prefix("TCS"), CommandName("fail-setup"), None)
      val id       = Id()
      val sequence = Sequence(id, Seq(command))

      val commandFailureMsg = "handle-setup-failed"
      val eventKey          = EventKey("tcs." + commandFailureMsg)

      val testProbe = createProbeFor(eventKey)

      val submitResponseF: Future[SubmitResponse] = setup.sequencer ? (SubmitSequenceAndWait(sequence, _))
      val error                                   = submitResponseF.futureValue.asInstanceOf[CommandResponse.Error]
      error.runId shouldBe id
      error.message.contains(commandFailureMsg) shouldBe true

      // exception handler publishes a event with exception msg as event name
      val event = testProbe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe commandFailureMsg

      // assert that next sequence is accepted and executed properly
      val command1  = Setup(Prefix("TCS"), CommandName("successful-command"), None)
      val id1       = Id()
      val sequence1 = Sequence(id1, Seq(command1))

      val submitResponse1: Future[SubmitResponse] = setup.sequencer ? (SubmitSequenceAndWait(sequence1, _))
      submitResponse1.futureValue should ===(Completed(id1))
      setup.shutdownSequencer()
    }

    "invoke exception handler when handle-goOnline-failed | ESW-139" in {
      val reason    = "handle-goOnline-failed"
      val eventKey  = EventKey("tcs." + reason)
      val testProbe = createProbeFor(eventKey)

      val setup = new SequencerSetup(tcsPackageId, tcsObservingMode)

      (setup.sequencer ? GoOffline).awaitResult
      setup.sequencer ! GoOnline(TestProbe[GoOnlineResponse].ref)

      assertReason(testProbe, reason)

      setup.shutdownSequencer()
    }

    "invoke exception handler when handle-shutdown-failed" in {
      val reason    = "handle-shutdown-failed"
      val eventKey  = EventKey("tcs." + reason)
      val testProbe = createProbeFor(eventKey)

      val setup                                       = new SequencerSetup(tcsPackageId, tcsObservingMode2)
      val eventualResponse: Future[GoOfflineResponse] = setup.sequencer ? GoOffline
      eventualResponse.awaitResult

      val probe = TestProbe[Ok.type]
      setup.sequencer ! Shutdown(probe.ref)

      assertReason(testProbe, reason)
      probe.expectMessage(Ok)
    }
  }

  private class SequencerSetup(id: String, mode: String) {
    val wiring                    = new SequencerWiring(id, mode, None)
    val sequencer                 = wiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
    def shutdownSequencer(): Unit = wiring.sequencerServer.shutDown().futureValue
  }

  private def assertReason(probe: TestProbe[Event], reason: String): Unit = {
    eventually {
      val event = probe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe reason
    }
  }

  private def createProbeFor(eventKey: EventKey): TestProbe[Event] = {
    val testProbe    = TestProbe[Event]
    val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
    subscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard msg
    testProbe
  }
}
