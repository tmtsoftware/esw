package esw.ocs.script

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.internal.{SequencerWiring, Timeouts}
import esw.utils.csw.LocationServiceUtils

class ScriptIntegrationTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  import frameworkTestKit._

  private implicit val typedSystem: ActorSystem[SpawnProtocol] = actorSystem
  implicit val scheduler: Scheduler                            = typedSystem.scheduler

  private implicit val askTimeout: Timeout = Timeouts.DefaultTimeout

  private val ocsSequencerId   = "testSequencerId4"
  private val ocsObservingMode = "testObservingMode4"

  private var locationService: LocationService           = _
  private var locationServiceUtils: LocationServiceUtils = _
  private var ocsWiring: SequencerWiring                 = _
  private var ocsSequencer: ActorRef[SequencerMsg]       = _

  private val tcsSequencer: ActorRef[SequencerMsg] = (typedSystem ? Spawn(TestSequencer.beh, "testSequencer")).awaitResult
  private val tcsSequencerId                       = "TCS"
  private val tcsObservingMode                     = "testObservingMode4"
  private val tcsConnection                        = AkkaConnection(ComponentId(s"$tcsSequencerId@$tcsObservingMode", ComponentType.Sequencer))
  private val tcsRegistration                      = AkkaRegistration(tcsConnection, Prefix("TCS.test"), tcsSequencer.toURI)
  private var sequenceReceivedByTCSProbe: Sequence = _

  "CswServices" must {
    "be able to send sequence to other Sequencer by resolving location through TestScript | ESW-195, ESW-119" in {
      locationService = HttpLocationServiceFactory.makeLocalClient
      locationServiceUtils = new LocationServiceUtils(locationService)
      locationServiceUtils.register(tcsRegistration).awaitResult

      ocsWiring = new SequencerWiring(ocsSequencerId, ocsObservingMode, None)
      ocsSequencer = ocsWiring.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]

      val command             = Setup(Prefix("TCS.test"), CommandName("command-4"), None)
      val submitResponseProbe = TestProbe[SubmitResponse]
      val sequenceId          = Id()
      val sequence            = Sequence(sequenceId, Seq(command))

      ocsSequencer ! LoadAndStartSequence(sequence, submitResponseProbe.ref)

      // This has to match with sequence created in TestScript -> handleSetupCommand("command-4")
      val assertableCommand =
        Setup(Id("testCommandIdString123"), Prefix("TCS.test"), CommandName("command-to-assert-on"), None, Set.empty)
      val assertableSequence = Sequence(Id("testSequenceIdString123"), Seq(assertableCommand))

      // response received by irisSequencer
      submitResponseProbe.expectMessage(Completed(sequenceId))

      // sequence sent to tcsSequencer by irisSequencer script
      eventually(sequenceReceivedByTCSProbe) shouldBe assertableSequence

      ocsWiring.shutDown().futureValue
      locationService.unregister(tcsConnection).futureValue
    }
  }

  object TestSequencer {
    def beh: Behaviors.Receive[SequencerMsg] = Behaviors.receiveMessage[SequencerMsg] {
      case LoadAndStartSequence(sequence, replyTo) =>
        sequenceReceivedByTCSProbe = sequence
        replyTo ! Started(sequence.runId)
        Behaviors.same
      case _ => Behaviors.same
    }
  }
}
