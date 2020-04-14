package esw.ocs.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.event.client.EventServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandIssue.UnsupportedCommandInStateIssue
import csw.params.commands.CommandResponse.{Completed, Error, Invalid, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.Id
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.actor.messages.SequencerState.{Loaded, Offline}
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.Pending
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.EventServer

import scala.concurrent.Future

class SequencerClientIntegrationTest extends EswTestKit(EventServer) {
  private val subsystem     = ESW
  private val observingMode = "MoonNight"

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val command4 = Setup(Prefix("esw.test"), CommandName("command-4"), None)
  private val command5 = Setup(Prefix("esw.test"), CommandName("command-5"), None)
  private val command6 = Setup(Prefix("esw.test"), CommandName("command-6"), None)

  private var ocsSequencer: SequencerApi = _
  private var tcsSequencer: SequencerApi = _

  override protected def beforeEach(): Unit = {
    //ocs sequencer, starts with TestScript2
    spawnSequencer(subsystem, observingMode)

    ocsSequencer = sequencerClient(subsystem, observingMode)

    // tcs sequencer, starts with TestScript3
    val tcsSequencerId            = TCS
    val tcsSequencerObservingMode = "moonnight"
    spawnSequencer(tcsSequencerId, tcsSequencerObservingMode)
    tcsSequencer = sequencerClient(tcsSequencerId, tcsSequencerObservingMode)
  }

  override protected def afterEach(): Unit = shutdownAllSequencers()

  "LoadSequence, Start it and Query its response | ESW-145, ESW-154, ESW-221, ESW-194, ESW-158, ESW-222, ESW-101, ESW-244" in {
    val sequence = Sequence(command1, command2)

    ocsSequencer.loadSequence(sequence).futureValue should ===(Ok)
    val startedResponse = ocsSequencer.startSequence().futureValue
    startedResponse shouldBe a[Started]
    eventually(ocsSequencer.query(startedResponse.runId).futureValue should ===(Completed(startedResponse.runId)))

    val step1         = Step(command1, Success, hasBreakpoint = false)
    val step2         = Step(command2, Success, hasBreakpoint = false)
    val expectedSteps = List(step1, step2)

    val expectedSequence = StepList(expectedSteps)

    val actualSequenceResponse = ocsSequencer.getSequence.futureValue.get
    val actualSteps = actualSequenceResponse.steps.zipWithIndex.map {
      case (step, index) => step.withId(expectedSteps(index).id)
    }

    actualSteps should ===(expectedSequence.steps)

    // assert sequencer does not accept LoadSequence/Start/QuerySequenceResponse messages in offline state
    ocsSequencer.goOffline().futureValue should ===(Ok)
    ocsSequencer.loadSequence(sequence).futureValue should ===(Unhandled(Offline.entryName, "LoadSequence"))
    val invalidId = Id("IdNotAvailable")

    val invalidStartResponse =
      Invalid(invalidId, UnsupportedCommandInStateIssue(Unhandled(Offline.entryName, "StartSequence").msg))
    ocsSequencer.startSequence().futureValue should ===(invalidStartResponse)

    ocsSequencer.queryFinal(invalidId).futureValue shouldBe a[Invalid]
  }

  "Load, Add commands and Start sequence - ensures sequence doesn't start on loading | ESW-222, ESW-101" in {

    val sequence = Sequence(command1)

    ocsSequencer.loadSequence(sequence).futureValue should ===(Ok)

    ocsSequencer.add(List(command2)).futureValue should ===(Ok)

    compareStepList(
      ocsSequencer.getSequence.futureValue,
      Some(StepList(List(Step(command1), Step(command2))))
    )

    ocsSequencer.startSequence().futureValue shouldBe a[Started]

    val expectedFinishedSteps = List(
      Step(command1, Success, hasBreakpoint = false),
      Step(command2, Success, hasBreakpoint = false)
    )
    eventually(
      compareStepList(ocsSequencer.getSequence.futureValue, Some(StepList(expectedFinishedSteps)))
    )
  }

  "SubmitSequenceAndWait for a sequence and execute commands that are added later | ESW-145, ESW-154, ESW-222" in {
    val sequence = Sequence(command1, command2)

    val processSeqResponse: Future[SubmitResponse] = ocsSequencer.submitAndWait(sequence)
    eventually(ocsSequencer.getSequence.futureValue shouldBe a[Some[_]])

    ocsSequencer.add(List(command3)).futureValue should ===(Ok)
    processSeqResponse.futureValue shouldBe a[Completed]

    compareStepList(
      ocsSequencer.getSequence.futureValue,
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
    // TestScript.scala returns Error on receiving command with prefix "fail-command"
    val command2 = Setup(Prefix("esw.test"), CommandName(failCommandName), None)
    val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
    val sequence = Sequence(command1, command2, command3)

    val submitResponseF = ocsSequencer.submitAndWait(sequence)
    eventually(ocsSequencer.getSequence.futureValue should not be empty)

    submitResponseF.futureValue shouldBe an[Error]

    compareStepList(
      ocsSequencer.getSequence.futureValue,
      Some(
        StepList(
          List(
            Step(command1, Success, hasBreakpoint = false),
            Step(command2, Failure(failCommandName), hasBreakpoint = false),
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
    ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed] // asserting the response
    //#################

    // creating subscriber for offline event
    val testProbe                = TestProbe[Event]
    val offlineKey               = EventKey("tcs.test.offline")
    val offlineEventSubscription = eventSubscriber.subscribeActorRef(Set(offlineKey), testProbe.ref)
    offlineEventSubscription.ready().futureValue
    testProbe.expectMessageType[SystemEvent] // discard invalid event
    //##############

    // assert ocs sequencer is in offline state on sending goOffline message
    ocsSequencer.goOffline().futureValue should ===(Ok)
    ocsSequencer.isOnline.futureValue should ===(false)

    // assert ocs sequencer does not accept editor commands in offline state
    ocsSequencer.add(List(command3)).futureValue should ===(Unhandled(Offline.entryName, "Add"))

    Thread.sleep(1000) // wait till goOffline msg from sequencer1 reaches to sequencer2

    //tcs sequencer should go in offline mode
    tcsSequencer.isOnline.futureValue should ===(false)

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

    ocsSequencer.goOnline().futureValue should ===(Ok)
    ocsSequencer.isOnline.futureValue should ===(true)

    Thread.sleep(1000) // wait till goOnline msg from sequencer1 reaches to sequencer2

    //tcs sequencer should go in online mode
    tcsSequencer.isOnline.futureValue should ===(true)

    // assert tcs sequencer's online handlers are called
    val onlineEvent = testProbe.expectMessageType[SystemEvent]
    onlineEvent.paramSet.head.values.head shouldBe "online"
  }

  "LoadSequence, Start it and Abort sequence | ESW-155, ESW-137" in {
    val sequence = Sequence(command4, command5, command6)

    ocsSequencer.loadSequence(sequence).futureValue should ===(Ok)

    //assert that it does not accept AbortSequence in loaded state
    ocsSequencer.abortSequence().futureValue should ===(Unhandled(Loaded.entryName, "AbortSequence"))

    val startedResponse = ocsSequencer.startSequence().futureValue
    startedResponse shouldBe a[Started]

    //assert that AbortSequence is accepted in InProgress state
    ocsSequencer.abortSequence().futureValue should ===(Ok)

    val expectedSteps = List(
      Step(command4, Success, hasBreakpoint = false)
    )
    val expectedSequence = Some(StepList(expectedSteps))
    val expectedResponse = Completed(startedResponse.runId)
    ocsSequencer.queryFinal(startedResponse.runId).futureValue should ===(expectedResponse)
    compareStepList(ocsSequencer.getSequence.futureValue, expectedSequence)
  }

  "LoadSequence, Start it and Stop | ESW-156, ESW-138" in {
    val sequence = Sequence(command4, command5, command6)

    ocsSequencer.loadSequence(sequence).futureValue should ===(Ok)

    //assert that it does not accept Stop in loaded state
    ocsSequencer.stop().futureValue should ===(Unhandled(Loaded.entryName, "Stop"))

    val startedResponse = ocsSequencer.startSequence().futureValue
    startedResponse shouldBe a[Started]

    //assert that Stop is accepted in InProgress state
    ocsSequencer.stop().futureValue should ===(Ok)

    val expectedSteps = List(
      Step(command4, Success, hasBreakpoint = false)
    )
    val expectedSequence = Some(StepList(expectedSteps))
    val expectedResponse = Completed(startedResponse.runId)
    ocsSequencer.queryFinal(startedResponse.runId).futureValue should ===(expectedResponse)
    compareStepList(ocsSequencer.getSequence.futureValue, expectedSequence)
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
    ocsSequencer.diagnosticMode(startTime, hint).futureValue should ===(Ok)

    val expectedDiagnosticEvent = testProbe.expectMessageType[SystemEvent]

    expectedDiagnosticEvent.paramSet.head shouldBe diagnosticModeParam

    //Operations Mode
    val operationsModeParam = StringKey.make("mode").set("operations")

    ocsSequencer.operationsMode().futureValue should ===(Ok)

    val expectedOperationsEvent = testProbe.expectMessageType[SystemEvent]

    expectedOperationsEvent.paramSet.head shouldBe operationsModeParam
  }

  "GetSequenceComponent | ESW-255" in {
    //start sequence component
    val sequenceComponentLocation = spawnSequenceComponent(ESW, Some("primary")).toOption.get
    val sequenceComponentImpl     = new SequenceComponentImpl(sequenceComponentLocation)

    //start sequencer
    val observingMode = "darknight"
    sequenceComponentImpl.loadScript(ESW, observingMode).futureValue.response.toOption.get

    val sequencer: SequencerApi = sequencerClient(ESW, observingMode)

    //assert that getSequenceComponent returns sequenceComponentLocation where sequencer is running
    sequencer.getSequenceComponent.futureValue should ===(sequenceComponentLocation)

    //clean-up
    sequenceComponentImpl.unloadScript()
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
