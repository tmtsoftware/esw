package esw.ocs.app

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{SequencerMsg, SubmitSequenceAndWait}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.Pending
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.messages.SequencerState.Offline
import mscoket.impl.post.PostClient
import mscoket.impl.ws.WebsocketClient

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps

class SequencerAdminIntegrationTest
    extends ScalaTestFrameworkTestKit(EventServer)
    with BaseTestSuite
    with SequencerAdminHttpCodecs {

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
    wiring.sequencerServer.start()
    val componentId     = ComponentId(s"$sequencerId@$observingMode@http", ComponentType.Service)
    val uri             = locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get.uri
    val httpUrl         = s"${uri.toString}post"
    val wsUrl           = s"ws://${uri.getHost}:${uri.getPort}/websocket"
    val postClient      = new PostClient[SequencerAdminPostRequest](httpUrl)
    val websocketClient = new WebsocketClient[SequencerAdminWebsocketRequest](wsUrl)

    sequencerAdmin = new SequencerAdminClient(postClient, websocketClient)
    sequencer = resolveSequencer()
  }

  override protected def afterEach(): Unit = {
    wiring.sequencerServer.shutDown().futureValue
  }

  "LoadSequence, Start it and Query its response | ESW-145, ESW-154, ESW-221, ESW-194, ESW-158, ESW-222, ESW-101" in {
    val sequence = Sequence(command1, command2)

    sequencerAdmin.loadSequence(sequence).futureValue should ===(Ok)
    sequencerAdmin.startSequence.futureValue should ===(Ok)
    sequencerAdmin.queryFinal.futureValue should ===(SequenceResult(Completed(sequence.runId)))

    val expectedSteps = List(
      Step(command1, Success(Completed(command1.runId)), hasBreakpoint = false),
      Step(command2, Success(Completed(command2.runId)), hasBreakpoint = false)
    )
    val expectedSequence = Some(StepList(sequence.runId, expectedSteps))
    sequencerAdmin.getSequence.futureValue should ===(expectedSequence)

    // assert sequencer does not accept LoadSequence/Start/QuerySequenceResponse messages in offline state
    sequencerAdmin.goOffline().futureValue should ===(Ok)
    sequencerAdmin.loadSequence(sequence).futureValue should ===(Unhandled(Offline.entryName, "LoadSequence"))

    sequencerAdmin.startSequence.futureValue should ===(Unhandled(Offline.entryName, "StartSequence"))
    sequencerAdmin.queryFinal.futureValue should ===(Unhandled(Offline.entryName, "QueryFinal"))
  }

  "Load, Add commands and Start sequence - ensures sequence doesn't start on loading | ESW-222, ESW-101" in {
    val sequence = Sequence(command1)

    sequencerAdmin.loadSequence(sequence).futureValue should ===(Ok)

    sequencerAdmin.add(List(command2)).futureValue should ===(Ok)

    sequencerAdmin.getSequence.futureValue should ===(Some(StepList(sequence.runId, List(Step(command1), Step(command2)))))

    sequencerAdmin.startSequence.futureValue should ===(Ok)

    val expectedFinishedSteps = List(
      Step(command1, Success(Completed(command1.runId)), hasBreakpoint = false),
      Step(command2, Success(Completed(command2.runId)), hasBreakpoint = false)
    )
    eventually(sequencerAdmin.getSequence.futureValue should ===(Some(StepList(sequence.runId, expectedFinishedSteps))))

  }

  "SubmitSequenceAndWait for a sequence and execute commands that are added later | ESW-145, ESW-154, ESW-222" in {
    val sequence = Sequence(command1, command2)

    val processSeqResponse: Future[SubmitResponse] = sequencer ? (SubmitSequenceAndWait(sequence, _))
    eventually(sequencerAdmin.getSequence.futureValue shouldBe a[Some[_]])

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

  "Short circuit on first failed command and getEvent failed sequence response | ESW-158, ESW-145, ESW-222" in {
    val failCommandName = "fail-command"

    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    // TestScript.scala returns Error on receiving command with name "fail-command"
    val command2 = Setup(Prefix("esw.test"), CommandName(failCommandName), None)
    val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
    val sequence = Sequence(command1, command2, command3)

    val processSeqResponse: Future[SubmitResponse] = sequencer ? (SubmitSequenceAndWait(sequence, _))
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

  "Go online and offline | ESW-194, ESW-222, ESW-101" in {
    val sequence = Sequence(command1, command2)

    val seqResponse: Future[SubmitResponse] = sequencer ? (SubmitSequenceAndWait(sequence, _))
    seqResponse.futureValue should ===(Completed(sequence.runId))

    // assert sequencer goes offline and offline handlers are called
    sequencerAdmin.goOffline().futureValue should ===(Ok)
    val offlineEvent = wiring.cswServices.getEvent("TCS.test.offline").asScala.futureValue.asScala.head
    offlineEvent.paramType.exists(BooleanKey.make("offline")) should ===(true)

    // assert sequencer does not accept editor commands in offline state
    sequencerAdmin.add(List(command3)).futureValue should ===(Unhandled(Offline.entryName, "Add"))

    // assert sequencer goes online and online handlers are called
    sequencerAdmin.goOnline().futureValue should ===(Ok)
    sequencerAdmin.isOnline.futureValue should ===(true)

    val onlineEvent = wiring.cswServices.getEvent("TCS.test.online").asScala.futureValue.asScala.head
    onlineEvent.paramType.exists(BooleanKey.make("online")) should ===(true)

    sequencerAdmin.loadSequence(sequence).futureValue should ===(Ok)
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
