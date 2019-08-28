package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.Pending
import esw.ocs.api.models.responses._
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.client.SequencerAdminClient
import esw.ocs.client.messages.SequencerMessages._
import esw.ocs.client.messages.SequencerState.Offline
import esw.ocs.internal.SequencerWiring

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerIntegrationTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {

  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private implicit val askTimeout: Timeout  = Timeout(10.seconds)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val sequencerId   = "testSequencerId1"
  private val observingMode = "testObservingMode1"

  val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)

  private var locationService: LocationService     = _
  private var wiring: SequencerWiring              = _
  private var sequencer: ActorRef[SequencerMsg]    = _
  private var sequencerAdmin: SequencerAdminClient = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService = HttpLocationServiceFactory.makeLocalClient
  }

  override protected def beforeEach(): Unit = {
    wiring = new SequencerWiring("testSequencerId1", "testObservingMode1", None)
    wiring.start()
    sequencer = resolveSequencer()
    sequencerAdmin = new SequencerAdminClient(sequencer)(sys, askTimeout)
  }

  override protected def afterEach(): Unit = {
    wiring.shutDown().futureValue
  }

  "Load a sequence and Start it | ESW-145, ESW-154" in {
    val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
    val sequence = Sequence(command3)

    val loadResponse: Future[LoadSequenceResponse] = sequencer ? (LoadSequence(sequence, _))
    loadResponse.futureValue should ===(Ok)

    val seqResponse: Future[SequenceResponse] = sequencer ? StartSequence
    seqResponse.futureValue should ===(SequenceResult(Completed(sequence.runId)))
  }

  "LoadAndStart a sequence and execute commands that are added later | ESW-145, ESW-154" in {
    val sequence = Sequence(command1, command2)

    val processSeqResponse: Future[SubmitResponse] = sequencer ? (LoadAndStartSequence(sequence, _))
    eventually((sequencer ? GetSequence).futureValue shouldBe a[Some[_]])

    sequencerAdmin.add(List(command3)).futureValue should ===(Ok)
    processSeqResponse.futureValue should ===(Completed(sequence.runId))

    sequencerAdmin.getSequence.futureValue should ===(
      Some(
        StepList(
          sequence.runId,
          List(
            Step(command1, Success(Completed(command1.runId)), hasBreakpoint = false),
            Step(command2, Success(Completed(command2.runId)), hasBreakpoint = false),
            Step(command3, Success(Completed(command3.runId)), hasBreakpoint = false)
          )
        )
      )
    )
  }

  "Short circuit on first failed command and get failed sequence response | ESW-158, ESW-145" in {
    val failCommandName = "fail-command"

    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    // TestScript.scala returns Error on receiving command with name "fail-command"
    val command2 = Setup(Prefix("esw.test"), CommandName(failCommandName), None)
    val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
    val sequence = Sequence(command1, command2, command3)

    val processSeqResponse: Future[SubmitResponse] = sequencer ? (LoadAndStartSequence(sequence, _))
    eventually(sequencerAdmin.getSequence.futureValue shouldBe a[Some[_]])

    processSeqResponse.futureValue should ===(Error(sequence.runId, failCommandName))

    sequencerAdmin.getSequence.futureValue should ===(
      Some(
        StepList(
          sequence.runId,
          List(
            Step(command1, Success(Completed(command1.runId)), hasBreakpoint = false),
            Step(command2, Failure(Error(command2.runId, failCommandName)), hasBreakpoint = false),
            Step(command3, Pending, hasBreakpoint = false)
          )
        )
      )
    )
  }

  "Go online and offline | ESW-194" in {
    val sequence = Sequence(command1, command2)

    val seqResponse: Future[SubmitResponse] = sequencer ? (LoadAndStartSequence(sequence, _))
    seqResponse.futureValue should ===(Completed(sequence.runId))

    // assert sequencer goes offline and offline handlers are called
    sequencerAdmin.goOffline().futureValue should ===(Ok)
    val offlineEvent = wiring.cswServicesWiring.eventServiceDsl.get("TCS.test.offline").futureValue.head
    offlineEvent.paramType.exists(BooleanKey.make("offline")) should ===(true)

    // assert sequencer does not accept editor commands in offline state
    sequencerAdmin.add(List(command3)).futureValue should ===(Unhandled(Offline.entryName, "Add"))

    // assert sequencer goes online and online handlers are called
    sequencerAdmin.goOnline().futureValue should ===(Ok)
    sequencerAdmin.isOnline.futureValue should ===(true)

    val onlineEvent = wiring.cswServicesWiring.eventServiceDsl.get("TCS.test.online").futureValue.head
    onlineEvent.paramType.exists(BooleanKey.make("online")) should ===(true)

    // assert sequencer can load a new sequence after going online
    val loadSeqResponse: Future[LoadSequenceResponse] = sequencer ? (LoadSequence(sequence, _))
    loadSeqResponse.futureValue should ===(Ok)
  }

  private def resolveSequencer(): ActorRef[SequencerMsg] =
    locationService
      .resolve(AkkaConnection(ComponentId(s"$sequencerId@$observingMode", ComponentType.Sequencer)), 5.seconds)
      .futureValue
      .value
      .uri
      .toActorRef
      .unsafeUpcast[SequencerMsg]
}
