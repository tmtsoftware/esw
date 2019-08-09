package esw.ocs.script

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.util.Timeout
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.internal.{SequencerWiring, Timeouts}

class ScriptIntegrationTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  private implicit val system: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior)
  implicit val scheduler: Scheduler                       = system.scheduler

  private implicit val timeout: Timeout  = Timeouts.DefaultTimeout
  private implicit val mat: Materializer = ActorMaterializer()

  private val irisSequencerId   = "testSequencerId4"
  private val irisObservingMode = "testObservingMode4"

  private var locationService: LocationService      = _
  private var irisWiring: SequencerWiring           = _
  private var irisSequencer: ActorRef[SequencerMsg] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService = HttpLocationServiceFactory.makeLocalClient
  }

  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }

  private val tcsSequencer: ActorRef[SequencerMsg]      = (system ? Spawn(testSequencerBeh, "testSequencer")).awaitResult
  private val tcsConnection                             = AkkaConnection(ComponentId("TCS.test.sequencer1", ComponentType.Sequencer))
  private val tcsRegistration                           = AkkaRegistration(tcsConnection, Prefix("TCS.test"), tcsSequencer.toURI)
  private var tcsRegistrationResult: RegistrationResult = _
  private var sequenceReceivedByTCSProb: Sequence       = _

  override protected def beforeEach(): Unit = {
    irisWiring = new SequencerWiring(irisSequencerId, irisObservingMode, None)
    irisWiring.start()
    irisSequencer = resolveSequencer()
    tcsRegistrationResult = locationService.register(tcsRegistration).awaitResult
  }

  override protected def afterEach(): Unit = {
    irisWiring.shutDown().awaitResult
    tcsRegistrationResult.unregister().awaitResult
  }

  "CswServices" must {
    "be able to send sequence to other irisSequencer | ESW-195" in {
      val command            = Setup(Prefix("esw.test"), CommandName("command-4"), None)
      val submitResponseProb = TestProbe[SubmitResponse]
      val sequenceId         = Id()
      val sequence           = Sequence(sequenceId, Seq(command))

      irisSequencer ! LoadAndStartSequence(sequence, submitResponseProb.ref)

      // This has to match with sequence created in TestScript -> handleSetupCommand("command-4")
      val commandToAssertOn =
        Setup(Id("testCommandIdString123"), Prefix("esw.test"), CommandName("command-to-assert-on"), None, Set.empty)
      val sequenceToAssertOn = Sequence(Id("testSequenceIdString123"), Seq(commandToAssertOn))

      // response received by irisSequencer
      submitResponseProb.expectMessage(Completed(sequenceId))

      // sequence sent to tcsSequencer by irisSequencer script
      eventually(sequenceReceivedByTCSProb) shouldBe sequenceToAssertOn
    }
  }

  private def resolveSequencer(): ActorRef[SequencerMsg] =
    locationService
      .resolve(AkkaConnection(ComponentId(s"$irisSequencerId@$irisObservingMode", ComponentType.Sequencer)), timeout.duration)
      .futureValue
      .value
      .uri
      .toActorRef
      .unsafeUpcast[SequencerMsg]

  def testSequencerBeh: Behaviors.Receive[SequencerMsg] = Behaviors.receiveMessage[SequencerMsg] {
    case LoadAndStartSequence(sequence, replyTo) =>
      sequenceReceivedByTCSProb = sequence
      replyTo ! Started(sequence.runId)
      Behaviors.same
    case _ => Behaviors.same
  }
}
