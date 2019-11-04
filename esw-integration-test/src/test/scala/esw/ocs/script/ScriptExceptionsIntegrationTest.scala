package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.AskPattern.{Askable, _}
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{SequencerMsg, SubmitSequenceAndWait}
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandName, CommandResponse, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.Timeouts

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

  "Script" must {
    "invoke exception handlers when exception is thrown from handleSetup and must fail the command with message of given exception | ESW-139" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)

      val command  = Setup(Prefix("TCS"), CommandName("fail-setup"), None)
      val id       = Id()
      val sequence = Sequence(id, Seq(command))

      val commandFailureMsg = "setup-failed"
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
      val command1  = Setup(Prefix("TCS"), CommandName("next-command"), None)
      val id1       = Id()
      val sequence1 = Sequence(id1, Seq(command1))

      val submitResponse1: Future[SubmitResponse] = ocsSequencer ? (SubmitSequenceAndWait(sequence1, _))
      submitResponse1.futureValue should ===(Completed(id1))
    }

  }
}
