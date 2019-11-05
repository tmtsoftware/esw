package esw.ocs.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequenceAndWait
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
import esw.ocs.impl.messages.SequencerState.{Loaded, Offline}
import msocket.impl.Encoding.JsonText

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerAdminIntegrationTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {

  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol.Command] = actorSystem

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private implicit val askTimeout: Timeout = Timeout(10.seconds)

  private val packageId     = "nfarios"
  private val observingMode = "darknight"

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val command4 = Setup(Prefix("esw.test"), CommandName("command-4"), None)
  private val command5 = Setup(Prefix("esw.test"), CommandName("command-5"), None)
  private val command6 = Setup(Prefix("esw.test"), CommandName("command-6"), None)

  private var locationService: LocationService       = _
  private var wiring: SequencerWiring                = _
  private var secondSequencerWiring: SequencerWiring = _
  private var sequencer: ActorRef[SequencerMsg]      = _
  private var sequencerAdmin1: SequencerAdminClient  = _
  private var sequencerAdmin2: SequencerAdminClient  = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService = HttpLocationServiceFactory.makeLocalClient
  }

  override protected def beforeEach(): Unit = {
    //first sequencer, starts with TestScript2
    wiring = new SequencerWiring(packageId, observingMode, None)
    wiring.sequencerServer.start()

    sequencerAdmin1 = resolveSequencerAdmin(packageId, observingMode)
    sequencer = resolveSequencer()

    // second sequencer, starts with TestScript3
    val secondSequencerId            = "testSequencerId6"
    val secondSequencerObservingMode = "testObservingMode6"
    secondSequencerWiring = new SequencerWiring(secondSequencerId, secondSequencerObservingMode, None)
    secondSequencerWiring.sequencerServer.start()
    sequencerAdmin2 = resolveSequencerAdmin(secondSequencerId, secondSequencerObservingMode)
  }

  override protected def afterEach(): Unit = {
    wiring.sequencerServer.shutDown().futureValue
    // shutting down the second sequencer
    secondSequencerWiring.sequencerServer.shutDown().futureValue
  }

  "LoadSequence, Start it and Query its response | ESW-145, ESW-154, ESW-221, ESW-194, ESW-158, ESW-222, ESW-101" in {
    val sequence = Sequence(command1, command2)

    sequencerAdmin1.loadSequence(sequence).futureValue should ===(Ok)
    sequencerAdmin1.startSequence.futureValue should ===(Ok)
    sequencerAdmin1.queryFinal.futureValue should ===(SequenceResult(Completed(sequence.runId)))

    val step1         = Step(command1, Success, hasBreakpoint = false)
    val step2         = Step(command2, Success, hasBreakpoint = false)
    val expectedSteps = List(step1, step2)

    val expectedSequence = StepList(sequence.runId, expectedSteps)

    val actualSequenceResponse = sequencerAdmin1.getSequence.futureValue.get
    val actualSteps = actualSequenceResponse.steps.zipWithIndex.map {
      case (step, index) => step.withId(expectedSteps(index).id)
    }

    actualSteps should ===(expectedSequence.steps)

    // assert sequencer does not accept LoadSequence/Start/QuerySequenceResponse messages in offline state
    sequencerAdmin1.goOffline().futureValue should ===(Ok)
    sequencerAdmin1.loadSequence(sequence).futureValue should ===(Unhandled(Offline.entryName, "LoadSequence"))

    sequencerAdmin1.startSequence.futureValue should ===(Unhandled(Offline.entryName, "StartSequence"))
    sequencerAdmin1.queryFinal.futureValue should ===(Unhandled(Offline.entryName, "QueryFinalInternal"))
  }

  "Load, Add commands and Start sequence - ensures sequence doesn't start on loading | ESW-222, ESW-101" in {

    val sequence = Sequence(command1)

    sequencerAdmin1.loadSequence(sequence).futureValue should ===(Ok)

    sequencerAdmin1.add(List(command2)).futureValue should ===(Ok)

    compareStepList(sequencerAdmin1.getSequence.futureValue, Some(StepList(sequence.runId, List(Step(command1), Step(command2)))))

    sequencerAdmin1.startSequence.futureValue should ===(Ok)

    val expectedFinishedSteps = List(
      Step(command1, Success, hasBreakpoint = false),
      Step(command2, Success, hasBreakpoint = false)
    )
    eventually(compareStepList(sequencerAdmin1.getSequence.futureValue, (Some(StepList(sequence.runId, expectedFinishedSteps)))))
  }

  "SubmitSequenceAndWait for a sequence and execute commands that are added later | ESW-145, ESW-154, ESW-222" in {
    val sequence = Sequence(command1, command2)

    val processSeqResponse: Future[SubmitResponse] = sequencer ? (SubmitSequenceAndWait(sequence, _))
    eventually(sequencerAdmin1.getSequence.futureValue shouldBe a[Some[_]])

    sequencerAdmin1.add(List(command3)).futureValue should ===(Ok)
    processSeqResponse.futureValue should ===(Completed(sequence.runId))

    compareStepList(
      sequencerAdmin1.getSequence.futureValue,
      Some(
        StepList(
          sequence.runId,
          List(
            Step(command1, Success, hasBreakpoint = false),
            Step(command2, Success, hasBreakpoint = false),
            Step(command3, Success, hasBreakpoint = false)
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
    eventually(sequencerAdmin1.getSequence.futureValue should not be empty)

    processSeqResponse.futureValue shouldBe an[Error]

    processSeqResponse.futureValue.runId should ===(sequence.runId)

    compareStepList(
      sequencerAdmin1.getSequence.futureValue,
      Some(
        StepList(
          sequence.runId,
          List(
            Step(command1, Success, hasBreakpoint = false),
            Step(command2, Failure("java.lang.RuntimeException: " + failCommandName), hasBreakpoint = false),
            Step(command3, Pending, hasBreakpoint = false)
          )
        )
      )
    )
  }

  "Go online and offline | ESW-194, ESW-222, ESW-101, ESW-134, ESW-236" in {

    //****************** Go offline ******************************

    //sending sequence to first sequencer(TestScript2)
    val sequence                            = Sequence(command1, command2)
    val seqResponse: Future[SubmitResponse] = sequencer ? (SubmitSequenceAndWait(sequence, _))
    seqResponse.futureValue should ===(Completed(sequence.runId)) // asserting the response
    //#################

    // creating subscriber for offline event
    val testProbe                = TestProbe[Event]
    val offlineSubscriber        = wiring.cswWiring.eventService.defaultSubscriber
    val offlineKey               = EventKey("tcs.test.offline")
    val offlineEventSubscription = offlineSubscriber.subscribeActorRef(Set(offlineKey), testProbe.ref)
    offlineEventSubscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard invalid event
    //##############

    // assert first sequencer is in offline state on sending goOffline message
    sequencerAdmin1.goOffline().futureValue should ===(Ok)
    sequencerAdmin1.isOnline.futureValue should ===(false)

    // assert first sequencer does not accept editor commands in offline state
    sequencerAdmin1.add(List(command3)).futureValue should ===(Unhandled(Offline.entryName, "Add"))

    Thread.sleep(1000) // wait till goOffline msg from sequencer1 reaches to sequencer2

    //second sequencer should go in offline mode
    sequencerAdmin2.isOnline.futureValue should ===(false)

    // assert second sequencer's offline handlers are called
    val offlineEvent = testProbe.expectMessageType[SystemEvent]
    offlineEvent.paramSet.head.values.head shouldBe "offline"

    //****************** go online ******************************
    // assert both the sequencers goes online and online handlers are called

    // creating subscriber for online event
    val onlineSubscriber        = wiring.cswWiring.eventService.defaultSubscriber
    val onlineKey               = EventKey("tcs.test.online")
    val onlineEventSubscription = onlineSubscriber.subscribeActorRef(Set(onlineKey), testProbe.ref)
    onlineEventSubscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard invalid event

    sequencerAdmin1.goOnline().futureValue should ===(Ok)
    sequencerAdmin1.isOnline.futureValue should ===(true)

    Thread.sleep(1000) // wait till goOnline msg from sequencer1 reaches to sequencer2

    //second sequencer should go in online mode
    sequencerAdmin2.isOnline.futureValue should ===(true)

    // assert second sequencer's online handlers are called
    val onlineEvent = testProbe.expectMessageType[SystemEvent]
    onlineEvent.paramSet.head.values.head shouldBe "online"
  }

  "LoadSequence, Start it and Abort sequence | ESW-155, ESW-137" in {
    val sequence = Sequence(command4, command5, command6)

    sequencerAdmin1.loadSequence(sequence).futureValue should ===(Ok)

    //assert that it does not accept AbortSequence in loaded state
    sequencerAdmin1.abortSequence().futureValue should ===(Unhandled(Loaded.entryName, "AbortSequence"))

    sequencerAdmin1.startSequence.futureValue should ===(Ok)

    //assert that AbortSequence is accepted in InProgress state
    sequencerAdmin1.abortSequence().futureValue should ===(Ok)

    val expectedSteps = List(
      Step(command4, Success, hasBreakpoint = false)
    )
    val expectedSequence = Some(StepList(sequence.runId, expectedSteps))
    val expectedResponse = SequenceResult(Completed(sequence.runId))
    sequencerAdmin1.queryFinal.futureValue should ===(expectedResponse)
    compareStepList(sequencerAdmin1.getSequence.futureValue, expectedSequence)
  }

  "LoadSequence, Start it and Stop | ESW-156, ESW-138" in {
    val sequence = Sequence(command4, command5, command6)

    sequencerAdmin1.loadSequence(sequence).futureValue should ===(Ok)

    //assert that it does not accept Stop in loaded state
    sequencerAdmin1.stop().futureValue should ===(Unhandled(Loaded.entryName, "Stop"))

    sequencerAdmin1.startSequence.futureValue should ===(Ok)

    //assert that Stop is accepted in InProgress state
    sequencerAdmin1.stop().futureValue should ===(Ok)

    val expectedSteps = List(
      Step(command4, Success, hasBreakpoint = false),
      Step(command5, Success, hasBreakpoint = false),
      Step(command6, Success, hasBreakpoint = false)
    )
    val expectedSequence = Some(StepList(sequence.runId, expectedSteps))
    val expectedResponse = SequenceResult(Completed(sequence.runId))
    sequencerAdmin1.queryFinal.futureValue should ===(expectedResponse)
    compareStepList(sequencerAdmin1.getSequence.futureValue, expectedSequence)
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
    sequencerAdmin1.diagnosticMode(startTime, hint).futureValue should ===(Ok)

    val expectedDiagnosticEvent = testProbe.expectMessageType[SystemEvent]

    expectedDiagnosticEvent.paramSet.head shouldBe diagnosticModeParam

    //Operations Mode
    val operationsModeParam = StringKey.make("mode").set("operations")

    sequencerAdmin1.operationsMode().futureValue should ===(Ok)

    val expectedOperationsEvent = testProbe.expectMessageType[SystemEvent]

    expectedOperationsEvent.paramSet.head shouldBe operationsModeParam
  }

  private def compareStepList(actual: Option[StepList], expected: Option[StepList]): Unit = {
    if (expected.isEmpty) actual should ===(None)
    else {
      val actualStepList   = actual.get
      val expectedStepList = expected.get

      actualStepList.steps should have size expectedStepList.steps.size

      actualStepList.steps.zip(expectedStepList.steps).foreach {
        case (e, a) =>
          e.status should ===(a.status)
          e.command should ===(a.command)
          e.hasBreakpoint should ===(a.hasBreakpoint)
      }
    }
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

    SequencerAdminClientFactory.make(postUrl, wsUrl, JsonText, () => None)
  }
}
