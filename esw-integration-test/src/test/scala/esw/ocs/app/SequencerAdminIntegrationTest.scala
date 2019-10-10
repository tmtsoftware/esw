package esw.ocs.app

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{SequencerMsg, SubmitSequenceAndWait}
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.Pending
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.SequencerAdminClientFactory
import esw.ocs.impl.messages.SequencerState.Offline

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerAdminIntegrationTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {

  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private implicit val askTimeout: Timeout  = Timeout(10.seconds)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val packageId     = "nfarios"
  private val observingMode = "darknight"

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)

  private var locationService: LocationService           = _
  private var wiring: SequencerWiring                    = _
  private var secondSequencerWiring: SequencerWiring     = _
  private var sequencer: ActorRef[SequencerMsg]          = _
  private var sequencerAdmin: SequencerAdminClient       = _
  private var secondSequencerAdmin: SequencerAdminClient = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService = HttpLocationServiceFactory.makeLocalClient
  }

  override protected def beforeEach(): Unit = {
    wiring = new SequencerWiring(packageId, observingMode, None)
    wiring.sequencerServer.start()

    sequencerAdmin = resolveSequencerAdmin(packageId, observingMode)
    sequencer = resolveSequencer()

    // second sequencer initialization
    val secondSequencerId            = "testSequencerId6"
    val secondSequencerObservingMode = "testObservingMode6"
    secondSequencerWiring = new SequencerWiring(secondSequencerId, secondSequencerObservingMode, None)
    secondSequencerWiring.sequencerServer.start()
    secondSequencerAdmin = resolveSequencerAdmin(secondSequencerId, secondSequencerObservingMode)
  }

  override protected def afterEach(): Unit = {
    wiring.sequencerServer.shutDown().futureValue
    // shutting down the second sequencer
    secondSequencerWiring.sequencerServer.shutDown().futureValue
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

  "Go online and offline | ESW-194, ESW-222, ESW-101, ESW-134" in {

    //****************** Go offline ******************************

    //sending sequence to first sequencer
    val sequence                            = Sequence(command1, command2)
    val seqResponse: Future[SubmitResponse] = sequencer ? (SubmitSequenceAndWait(sequence, _))
    seqResponse.futureValue should ===(Completed(sequence.runId)) // asserting the response
    //#################

    // creating subscriber for offline event
    val testProbe1               = TestProbe[Event]
    val offlineSubscriber        = wiring.cswWiring.eventService.defaultSubscriber
    val offlineKey               = EventKey("tcs.test.offline")
    val offlineEventSubscription = offlineSubscriber.subscribeActorRef(Set(offlineKey), testProbe1.ref)
    offlineEventSubscription.ready().futureValue
    testProbe1.expectMessageType[SystemEvent] // discard invalid event
    //##############

    // assert first sequencer is in offline state on sending goOffline message
    sequencerAdmin.goOffline().futureValue should ===(Ok)
    sequencerAdmin.isOnline.futureValue should ===(false)
    //##############
    Thread.sleep(1000)

    //second sequencer should go in offline mode
    secondSequencerAdmin.isOnline.futureValue should ===(false)

    // assert sequencer's offline handlers are called
    val offlineEvent = testProbe1.expectMessageType[SystemEvent]
    offlineEvent.paramSet.head.values.head shouldBe "offline"

    // assert sequencer does not accept editor commands in offline state
    sequencerAdmin.add(List(command3)).futureValue should ===(Unhandled(Offline.entryName, "Add"))

    //****************** go online ******************************
    // assert sequencer goes online and online handlers are called

    // creating subscriber for online event
    val testProbe2              = TestProbe[Event]
    val onlineSubscriber        = wiring.cswWiring.eventService.defaultSubscriber
    val onlineKey               = EventKey("tcs.test.online")
    val onlineEventSubscription = onlineSubscriber.subscribeActorRef(Set(onlineKey), testProbe2.ref)
    onlineEventSubscription.ready().futureValue
    testProbe2.expectMessageType[SystemEvent] // discard invalid event

    sequencerAdmin.goOnline().futureValue should ===(Ok)
    sequencerAdmin.isOnline.futureValue should ===(true)
    Thread.sleep(1000)

    //second sequencer should go in online mode
    secondSequencerAdmin.isOnline.futureValue should ===(true)

    // assert sequencer's online handlers are called
    val onlineEvent = testProbe2.expectMessageType[SystemEvent]
    onlineEvent.paramSet.head.values.head shouldBe "online"
  }

  "DiagnosticMode and OperationsMode| ESW-143, ESW-134" in {
    val startTime = UTCTime.now()
    val hint      = "engineering"

    val diagnosticModeParam = StringKey.make("mode").set("diagnostic")

    val eventService       = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
    val diagnosticEventKey = EventKey(Prefix("tcs.test"), EventName("diagnostic-data"))

    val testProbe                   = TestProbe[Event]
    val diagnosticEventSubscription = eventService.defaultSubscriber.subscribeActorRef(Set(diagnosticEventKey), testProbe.ref)
    diagnosticEventSubscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard invalid event

    //Diagnostic Mode
    sequencerAdmin.diagnosticMode(startTime, hint).futureValue should ===(Ok)

    val expectedDiagnosticEvent = testProbe.expectMessageType[SystemEvent]

    expectedDiagnosticEvent.paramSet.head shouldBe diagnosticModeParam

    //Operations Mode
    val operationsModeParam = StringKey.make("mode").set("operations")

    sequencerAdmin.operationsMode().futureValue should ===(Ok)

    val expectedOperationsEvent = testProbe.expectMessageType[SystemEvent]

    expectedOperationsEvent.paramSet.head shouldBe operationsModeParam
  }

  private def resolveSequencer(): ActorRef[SequencerMsg] =
    locationService
      .resolve(AkkaConnection(ComponentId(s"$packageId@$observingMode", ComponentType.Sequencer)), 5.seconds)
      .futureValue
      .value
      .uri
      .toActorRef
      .unsafeUpcast[SequencerMsg]

  private def resolveSequencerAdmin(packageId: String, observingMode: String): SequencerAdminClient = {
    val componentId = ComponentId(s"$packageId@$observingMode@http", ComponentType.Service)
    val uri         = locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get.uri
    val postUrl     = s"${uri.toString}post-endpoint"
    val wsUrl       = s"ws://${uri.getHost}:${uri.getPort}/websocket-endpoint"

    SequencerAdminClientFactory.make(postUrl, wsUrl, None)
  }
}
