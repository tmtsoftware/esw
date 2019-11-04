package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{SequencerMsg, SubmitSequenceAndWait}
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandName, CommandResponse, Observe, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol._
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.Timeouts
import esw.ocs.impl.messages.SequencerMessages._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ScriptExceptionsIntegrationTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem

  private implicit val askTimeout: Timeout = Timeouts.DefaultTimeout

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  // TestScript.kt
  private val ocsPackageId     = "esw"
  private val ocsObservingMode = "exceptionscript"

  private var ocsWiring: SequencerWiring           = _
  private var ocsSequencer: ActorRef[SequencerMsg] = _

  override def beforeEach(): Unit = {
    ocsWiring = new SequencerWiring(ocsPackageId, ocsObservingMode, None)
    ocsSequencer = ocsWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
  }

  override def afterEach(): Unit = {
    ocsWiring.sequencerServer.shutDown().futureValue
  }

  val failSetupCommand    = Setup(Prefix("TCS"), CommandName("fail-setup"), None)
  val failSetupId         = Id()
  val failSetupSequence   = Sequence(failSetupId, Seq(failSetupCommand))
  val failObserveCommand  = Observe(Prefix("TCS"), CommandName("fail-observe"), None)
  val failObserveId       = Id()
  val failObserveSequence = Sequence(failObserveId, Seq(failObserveCommand))

  val idleStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
    ("sequencer msg", "failure msg"),
    (SubmitSequenceAndWait(failSetupSequence, TestProbe[SubmitResponse].ref), "handle-setup-failed"),
    (SubmitSequenceAndWait(failObserveSequence, TestProbe[SubmitResponse].ref), "handle-observe-failed"),
    (GoOffline(TestProbe[GoOfflineResponse].ref), "handle-goOffline-failed"),
    // fixme : uncomment and fix.
    //    (Shutdown(TestProbe[Ok.type].ref), "handle-shutdown-failed"),
    //    (DiagnosticMode(UTCTime.now(), "any", TestProbe[DiagnosticModeResponse].ref), "handle-diagnostic-mode-failed"),
    (OperationsMode(TestProbe[OperationsModeResponse].ref), "handle-operations-mode-failed")
  )

  val inProgressStateTestCases: TableFor2[SequencerMsg, String] = Table.apply(
    ("sequencer msg", "failure msg"),
    (Stop(TestProbe[OkOrUnhandledResponse].ref), "handle-stop-failed"),
    (AbortSequence(TestProbe[OkOrUnhandledResponse].ref), "handle-abort-failed")
  )

  "Script" must {
    "invoke exception handlers when exception is thrown from handler and must fail the command with message of given exception | ESW-139" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)

      val command  = Setup(Prefix("TCS"), CommandName("fail-setup"), None)
      val id       = Id()
      val sequence = Sequence(id, Seq(command))

      val commandFailureMsg = "handle-setup-failed"
      val eventKey          = EventKey("tcs." + commandFailureMsg)

      val testProbe    = TestProbe[Event]
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      val submitResponseF: Future[SubmitResponse] = ocsSequencer ? (SubmitSequenceAndWait(sequence, _))
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

      val submitResponse1: Future[SubmitResponse] = ocsSequencer ? (SubmitSequenceAndWait(sequence1, _))
      submitResponse1.futureValue should ===(Completed(id1))
    }

    forAll(idleStateTestCases) { (msg, reason) =>
      s"invoke exception handler when ${reason}" in {
        val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
        val eventKey     = EventKey("tcs." + reason)

        val testProbe    = TestProbe[Event]
        val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
        subscription.ready().futureValue
        testProbe.expectMessageType[SystemEvent] // discard invalid event

        ocsSequencer ! msg

        // exception handler publishes a event with exception msg as event name
        eventually {
          val event = testProbe.expectMessageType[SystemEvent]
          event.eventName.name shouldBe reason
        }
      }
    }

    forAll(inProgressStateTestCases) { (msg, reason) =>
      s"invoke exception handler when ${reason}" in {
        val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
        val eventKey     = EventKey("tcs." + reason)

        val testProbe    = TestProbe[Event]
        val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
        subscription.ready().futureValue
        testProbe.expectMessageType[SystemEvent] // discard invalid event

        val longRunningSetupCommand  = Setup(Prefix("TCS"), CommandName("long-running-setup"), None)
        val longRunningSetupSequence = Sequence(longRunningSetupCommand)

        ocsSequencer ? ((x: ActorRef[SubmitResponse]) => SubmitSequenceAndWait(longRunningSetupSequence, x))
        ocsSequencer ! msg

        // exception handler publishes a event with exception msg as event name
        eventually {
          val event = testProbe.expectMessageType[SystemEvent]
          event.eventName.name shouldBe reason
        }
      }
    }
  }
}
