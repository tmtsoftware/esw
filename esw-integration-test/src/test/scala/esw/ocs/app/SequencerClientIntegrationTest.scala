package esw.ocs.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.event.client.EventServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandIssue.UnsupportedCommandInStateIssue
import csw.params.commands.CommandResponse.{Completed, Error, Invalid, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.time.core.models.UTCTime
import esw.ocs.api.client.{SequencerAdminClient, SequencerCommandClient}
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.Pending
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.impl.messages.SequencerState.{Loaded, Offline}
import esw.ocs.testkit.EswTestKit

import scala.concurrent.Future

class SequencerClientIntegrationTest extends EswTestKit(EventServer) {
  private val packageId     = "ocs"
  private val observingMode = "moonnight"

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val command4 = Setup(Prefix("esw.test"), CommandName("command-4"), None)
  private val command5 = Setup(Prefix("esw.test"), CommandName("command-5"), None)
  private val command6 = Setup(Prefix("esw.test"), CommandName("command-6"), None)

  private var ocsSequencerAdmin: SequencerAdminClient        = _
  private var ocsSequencerCommandApi: SequencerCommandClient = _
  private var tcsSequencerAdmin: SequencerAdminClient        = _

  override protected def beforeEach(): Unit = {
    //ocs sequencer, starts with TestScript2
    spawnSequencer(packageId, observingMode)

    ocsSequencerAdmin = sequencerAdminClient(packageId, observingMode)
    ocsSequencerCommandApi = sequencerCommandClient(packageId, observingMode)

    // tcs sequencer, starts with TestScript3
    val tcsSequencerId            = "tcs"
    val tcsSequencerObservingMode = "moonnight"
    spawnSequencer(tcsSequencerId, tcsSequencerObservingMode)
    tcsSequencerAdmin = sequencerAdminClient(tcsSequencerId, tcsSequencerObservingMode)
  }

  override protected def afterEach(): Unit = shutdownAllSequencers()

  "LoadSequence, Start it and Query its response | ESW-145, ESW-154, ESW-221, ESW-194, ESW-158, ESW-222, ESW-101" in {
    val sequence = Sequence(command1, command2)

    ocsSequencerCommandApi.loadSequence(sequence).futureValue should ===(Ok)
    val startedResponse = ocsSequencerCommandApi.startSequence().futureValue
    startedResponse shouldBe a[Started]
    ocsSequencerCommandApi.queryFinal(startedResponse.runId).futureValue should ===(Completed(startedResponse.runId))

    val step1         = Step(command1, Success, hasBreakpoint = false)
    val step2         = Step(command2, Success, hasBreakpoint = false)
    val expectedSteps = List(step1, step2)

    val expectedSequence = StepList(expectedSteps)

    val actualSequenceResponse = ocsSequencerAdmin.getSequence.futureValue.get
    val actualSteps = actualSequenceResponse.steps.zipWithIndex.map {
      case (step, index) => step.withId(expectedSteps(index).id)
    }

    actualSteps should ===(expectedSequence.steps)

    // assert sequencer does not accept LoadSequence/Start/QuerySequenceResponse messages in offline state
    ocsSequencerCommandApi.goOffline().futureValue should ===(Ok)
    ocsSequencerCommandApi.loadSequence(sequence).futureValue should ===(Unhandled(Offline.entryName, "LoadSequence"))
    val invalidId = Id("IdNotAvailable")

    val invalidStartResponse =
      Invalid(invalidId, UnsupportedCommandInStateIssue(Unhandled(Offline.entryName, "StartSequence").msg))
    ocsSequencerCommandApi.startSequence().futureValue should ===(invalidStartResponse)

    ocsSequencerCommandApi.queryFinal(invalidId).futureValue shouldBe a[Error]
  }

  "Load, Add commands and Start sequence - ensures sequence doesn't start on loading | ESW-222, ESW-101" in {

    val sequence = Sequence(command1)

    ocsSequencerCommandApi.loadSequence(sequence).futureValue should ===(Ok)

    ocsSequencerAdmin.add(List(command2)).futureValue should ===(Ok)

    compareStepList(
      ocsSequencerAdmin.getSequence.futureValue,
      Some(StepList(List(Step(command1), Step(command2))))
    )

    ocsSequencerCommandApi.startSequence().futureValue shouldBe a[Started]

    val expectedFinishedSteps = List(
      Step(command1, Success, hasBreakpoint = false),
      Step(command2, Success, hasBreakpoint = false)
    )
    eventually(
      compareStepList(ocsSequencerAdmin.getSequence.futureValue, (Some(StepList(expectedFinishedSteps))))
    )
  }

  "SubmitSequenceAndWait for a sequence and execute commands that are added later | ESW-145, ESW-154, ESW-222" in {
    val sequence = Sequence(command1, command2)

    val processSeqResponse: Future[SubmitResponse] = ocsSequencerCommandApi.submitAndWait(sequence)
    eventually(ocsSequencerAdmin.getSequence.futureValue shouldBe a[Some[_]])

    ocsSequencerAdmin.add(List(command3)).futureValue should ===(Ok)
    processSeqResponse.futureValue shouldBe a[Completed]

    compareStepList(
      ocsSequencerAdmin.getSequence.futureValue,
      Some(
        StepList(
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

    val submitResponseF = ocsSequencerCommandApi.submitAndWait(sequence)
    eventually(ocsSequencerAdmin.getSequence.futureValue should not be empty)

    submitResponseF.futureValue shouldBe an[Error]

    compareStepList(
      ocsSequencerAdmin.getSequence.futureValue,
      Some(
        StepList(
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

    //sending sequence to ocs sequencer(TestScript2)
    val sequence = Sequence(command1, command2)
    ocsSequencerCommandApi.submitAndWait(sequence).futureValue shouldBe a[Completed] // asserting the response
    //#################

    // creating subscriber for offline event
    val testProbe                = TestProbe[Event]
    val offlineKey               = EventKey("tcs.test.offline")
    val offlineEventSubscription = eventSubscriber.subscribeActorRef(Set(offlineKey), testProbe.ref)
    offlineEventSubscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard invalid event
    //##############

    // assert ocs sequencer is in offline state on sending goOffline message
    ocsSequencerCommandApi.goOffline().futureValue should ===(Ok)
    ocsSequencerAdmin.isOnline.futureValue should ===(false)

    // assert ocs sequencer does not accept editor commands in offline state
    ocsSequencerAdmin.add(List(command3)).futureValue should ===(Unhandled(Offline.entryName, "Add"))

    Thread.sleep(1000) // wait till goOffline msg from sequencer1 reaches to sequencer2

    //tcs sequencer should go in offline mode
    tcsSequencerAdmin.isOnline.futureValue should ===(false)

    // assert tcs sequencer's offline handlers are called
    val offlineEvent = testProbe.expectMessageType[SystemEvent]
    offlineEvent.paramSet.head.values.head shouldBe "offline"

    //****************** go online ******************************
    // assert both the sequencers goes online and online handlers are called

    // creating subscriber for online event
    val onlineKey               = EventKey("tcs.test.online")
    val onlineEventSubscription = eventSubscriber.subscribeActorRef(Set(onlineKey), testProbe.ref)
    onlineEventSubscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard invalid event

    ocsSequencerCommandApi.goOnline().futureValue should ===(Ok)
    ocsSequencerAdmin.isOnline.futureValue should ===(true)

    Thread.sleep(1000) // wait till goOnline msg from sequencer1 reaches to sequencer2

    //tcs sequencer should go in online mode
    tcsSequencerAdmin.isOnline.futureValue should ===(true)

    // assert tcs sequencer's online handlers are called
    val onlineEvent = testProbe.expectMessageType[SystemEvent]
    onlineEvent.paramSet.head.values.head shouldBe "online"
  }

  "LoadSequence, Start it and Abort sequence | ESW-155, ESW-137" in {
    val sequence = Sequence(command4, command5, command6)

    ocsSequencerCommandApi.loadSequence(sequence).futureValue should ===(Ok)

    //assert that it does not accept AbortSequence in loaded state
    ocsSequencerAdmin.abortSequence().futureValue should ===(Unhandled(Loaded.entryName, "AbortSequence"))

    val startedResponse = ocsSequencerCommandApi.startSequence().futureValue
    startedResponse shouldBe a[Started]

    //assert that AbortSequence is accepted in InProgress state
    ocsSequencerAdmin.abortSequence().futureValue should ===(Ok)

    val expectedSteps = List(
      Step(command4, Success, hasBreakpoint = false)
    )
    val expectedSequence = Some(StepList(expectedSteps))
    val expectedResponse = Completed(startedResponse.runId)
    ocsSequencerCommandApi.queryFinal(startedResponse.runId).futureValue should ===(expectedResponse)
    compareStepList(ocsSequencerAdmin.getSequence.futureValue, expectedSequence)
  }

  "LoadSequence, Start it and Stop | ESW-156, ESW-138" in {
    val sequence = Sequence(command4, command5, command6)

    ocsSequencerCommandApi.loadSequence(sequence).futureValue should ===(Ok)

    //assert that it does not accept Stop in loaded state
    ocsSequencerAdmin.stop().futureValue should ===(Unhandled(Loaded.entryName, "Stop"))

    val startedResponse = ocsSequencerCommandApi.startSequence().futureValue
    startedResponse shouldBe a[Started]

    //assert that Stop is accepted in InProgress state
    ocsSequencerAdmin.stop().futureValue should ===(Ok)

    val expectedSteps = List(
      Step(command4, Success, hasBreakpoint = false),
      Step(command5, Success, hasBreakpoint = false),
      Step(command6, Success, hasBreakpoint = false)
    )
    val expectedSequence = Some(StepList(expectedSteps))
    val expectedResponse = Completed(startedResponse.runId)
    ocsSequencerCommandApi.queryFinal(startedResponse.runId).futureValue should ===(expectedResponse)
    compareStepList(ocsSequencerAdmin.getSequence.futureValue, expectedSequence)
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
    ocsSequencerCommandApi.diagnosticMode(startTime, hint).futureValue should ===(Ok)

    val expectedDiagnosticEvent = testProbe.expectMessageType[SystemEvent]

    expectedDiagnosticEvent.paramSet.head shouldBe diagnosticModeParam

    //Operations Mode
    val operationsModeParam = StringKey.make("mode").set("operations")

    ocsSequencerCommandApi.operationsMode().futureValue should ===(Ok)

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
}
