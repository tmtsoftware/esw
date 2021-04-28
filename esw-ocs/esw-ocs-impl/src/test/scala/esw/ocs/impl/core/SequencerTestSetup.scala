package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequence
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Completed, Started, SubmitResponse}
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.time.core.models.UTCTime
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState.{Idle, Running}
import esw.ocs.api.actor.messages.{SequenceComponentMsg, SequencerState}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.impl.script.ScriptApi
import org.mockito.MockitoSugar
import org.scalatest.Assertion
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Random, Success}

class SequencerTestSetup(sequence: Sequence)(implicit system: ActorSystem[_]) {
  import Matchers._
  import org.mockito.MockitoSugar._

  implicit private val patienceConfig: PatienceConfig = PatienceConfig(5.seconds)
  implicit val ec: ExecutionContext                   = system.executionContext

  val script: ScriptApi                             = mock[ScriptApi]
  private val componentId                           = mock[ComponentId]
  private val locationService                       = mock[LocationService]
  private val logger                                = mock[Logger]
  private val sequenceComponentPrefix: Prefix       = Prefix(ESW, "primary")
  private val seqCompAkkaConnection: AkkaConnection = AkkaConnection(ComponentId(sequenceComponentPrefix, SequenceComponent))
  private val sequenceComponentLocation: AkkaLocation = AkkaLocation(
    seqCompAkkaConnection,
    TestProbe[SequenceComponentMsg]().ref.toURI,
    Metadata.empty
  )
  private def mockShutdownHttpService: () => Future[Done.type] = () => Future { Done }
  when(locationService.unregister(AkkaConnection(componentId))).thenReturn(Future.successful(Done))
  when(script.executeShutdown()).thenReturn(Future.successful(Done))

  //for getSequenceComponent message
  when(locationService.find(seqCompAkkaConnection)).thenReturn(Future.successful(Some(sequenceComponentLocation)))

  val sequencerName = s"SequencerActor${Random.between(0, Int.MaxValue)}"
  when(componentId.prefix).thenReturn(Prefix(ESW, sequencerName))
  private val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, sequenceComponentPrefix, logger, mockShutdownHttpService)

  val sequencerActor: ActorRef[SequencerMsg] = system.systemActorOf(sequencerBehavior.setup, sequencerName)

  private def deadLetter = system.deadLetters

  private val completionPromise = Promise[SubmitResponse]()

  def loadSequenceAndAssertResponse(expected: OkOrUnhandledResponse): Unit = {
    val probe = TestProbe[OkOrUnhandledResponse]()
    sequencerActor ! LoadSequence(sequence, probe.ref)
    probe.expectMessage(expected)
  }

  def loadAndStartSequenceThenAssertRunning(): Assertion = {
    val probe = TestProbe[SequencerSubmitResponse]()

    when { script.executeNewSequenceHandler() }.thenAnswer(Future.successful(Done))

    sequencerActor ! SubmitSequenceInternal(sequence, probe.ref)

    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]()
    eventually {
      probe.expectMessageType[SequencerSubmitResponse]
      sequencerActor ! GetSequence(p.ref)
      val stepList = p.expectMessageType[Option[StepList]]
      stepList.isDefined shouldBe true
    }
  }

  def pullAllStepsAndAssertSequenceIsFinished(): Assertion = {
    eventually {
      val probe = TestProbe[Option[StepList]]()
      sequencerActor ! GetSequence(probe.ref)
      probe.expectMessageType[Option[StepList]]
    }

    pullAllSteps(sequencerActor)
    eventually(assertSequenceIsFinished())
  }

  def assertSequenceNotStarted(): Assertion = {
    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]()
    sequencerActor ! GetSequence(p.ref)
    eventually {
      val stepList = p.expectMessageType[Option[StepList]]
      stepList.get.steps.forall(s => s.isPending)
      stepList.isDefined shouldBe true
    }
  }

  def assertSequenceNotStartedAndLoaded(): Assertion = {
    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]()
    sequencerActor ! GetSequence(p.ref)
    eventually {
      val stepList = p.expectMessageType[Option[StepList]]
      stepList.isDefined shouldBe false
    }
  }

  def assertCurrentSequence(expected: Option[StepList]): Unit = {
    val probe = TestProbe[Option[StepList]]()
    sequencerActor ! GetSequence(probe.ref)
    val message = probe.receiveMessage()

    if (expected.isEmpty) message should ===(None)
    else {
      val actualStepList   = message.get
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

  def assertCurrentSequence(expected: StepList): Unit = assertCurrentSequence(Some(expected))

  def assertEngineCanExecuteNext(isReadyToExecuteNext: Boolean): Unit = {
    val probe = TestProbe[Ok.type]()
    sequencerActor ! ReadyToExecuteNext(probe.ref)
    if (isReadyToExecuteNext) {
      probe.expectMessage(Ok)
    }
    else {
      intercept[AssertionError] {
        //ReadyToExecuteNext won't respond and timeout will be captured by AssertionError
        probe.receiveMessage()
      }
    }
  }

  def abortSequenceAndAssertResponse(
      response: OkOrUnhandledResponse,
      expectedState: SequencerState[SequencerMsg]
  ): TestProbe[OkOrUnhandledResponse] = {
    val probe                          = TestProbe[OkOrUnhandledResponse]()
    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]()

    when(script.executeAbort()).thenReturn(Future.successful(Done))
    sequencerActor ! AbortSequence(probe.ref)

    //GetSequence msg while aborting sequence
    sequencerActor ! GetSequence(p.ref)

    probe.expectMessage(response)

    //GetSequence should be handled and return response while aborting sequence
    p.expectMessageType[Option[StepList]]

    //After abort sequence
    sequencerActor ! GetSequence(p.ref)
    val stepList = p.expectMessageType[Option[StepList]]
    expectedState match {
      case Idle                            => stepList shouldBe None
      case Running                         => stepList.get.nextPending shouldBe None
      case x: SequencerState[SequencerMsg] => assert(false, s"$x is not valid state after AbortSequence")
    }
    verify(script, timeout(1000)).executeAbort()
    probe
  }

  def stopAndAssertResponse(
      response: OkOrUnhandledResponse,
      expectedState: SequencerState[SequencerMsg]
  ): TestProbe[OkOrUnhandledResponse] = {
    val probe                          = TestProbe[OkOrUnhandledResponse]()
    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]()

    when(script.executeStop()).thenReturn(Future.successful(Done))
    sequencerActor ! Stop(probe.ref)

    //GetSequence msg while aborting sequence
    sequencerActor ! GetSequence(p.ref)

    probe.expectMessage(response)

    //GetSequence should be handled and return response while aborting sequence
    p.expectMessageType[Option[StepList]]

    //After stop sequence
    sequencerActor ! GetSequence(p.ref)
    val stepList = p.expectMessageType[Option[StepList]]
    expectedState match {
      case Idle                            => stepList shouldNot be(None)
      case Running                         => stepList shouldNot be(None)
      case x: SequencerState[SequencerMsg] => assert(false, s"$x is not valid state after Stop")
    }
    verify(script, timeout(1000)).executeStop()
    probe
  }

  def pauseAndAssertResponse(response: PauseResponse): PauseResponse = {
    val probe = TestProbe[PauseResponse]()
    sequencerActor ! Pause(probe.ref)
    probe.expectMessage(response)
  }

  def resumeAndAssertResponse(response: OkOrUnhandledResponse): OkOrUnhandledResponse = {
    val probe = TestProbe[OkOrUnhandledResponse]()
    sequencerActor ! Resume(probe.ref)
    probe.expectMessage(response)
  }

  def resetAndAssertResponse(response: OkOrUnhandledResponse): OkOrUnhandledResponse = {
    val probe = TestProbe[OkOrUnhandledResponse]()
    sequencerActor ! Reset(probe.ref)
    probe.expectMessage(response)
  }

  def replaceAndAssertResponse(idToReplace: Id, commands: List[SequenceCommand], response: GenericResponse): GenericResponse = {
    val replaceResProbe = TestProbe[GenericResponse]()
    sequencerActor ! Replace(idToReplace, commands, replaceResProbe.ref)
    replaceResProbe.expectMessage(response)
  }

  def addBreakpointAndAssertResponse(id: Id, response: GenericResponse): Unit = {
    val probe = TestProbe[GenericResponse]()
    sequencerActor ! AddBreakpoint(id, probe.ref)
    probe.expectMessage(response)
  }

  def removeBreakpointAndAssertResponse(id: Id, response: RemoveBreakpointResponse): Unit = {
    val probe = TestProbe[RemoveBreakpointResponse]()
    sequencerActor ! RemoveBreakpoint(id, probe.ref)
    probe.expectMessage(response)
  }

  def goOfflineAndAssertResponse(response: GoOfflineResponse): GoOfflineResponse = {
    val probe = TestProbe[GoOfflineResponse]()
    sequencerActor ! GoOffline(probe.ref)
    probe.expectMessage(response)
  }

  def goOnlineAndAssertResponse(response: GoOnlineResponse, handlerMockResponse: Future[Done]): GoOnlineResponse = {
    when(script.executeGoOnline()).thenReturn(handlerMockResponse)

    val probe = TestProbe[GoOnlineResponse]()
    sequencerActor ! GoOnline(probe.ref)
    probe.expectMessage(response)
  }

  def diagnosticModeAndAssertResponse(
      startTime: UTCTime,
      hint: String,
      response: DiagnosticModeResponse,
      handlerMockResponse: Future[Done]
  ): DiagnosticModeResponse = {
    when(script.executeDiagnosticMode(startTime, hint)).thenReturn(handlerMockResponse)

    val probe = TestProbe[DiagnosticModeResponse]()
    sequencerActor ! DiagnosticMode(startTime, hint, probe.ref)

    verify(script, timeout(1000)).executeDiagnosticMode(startTime, hint)
    probe.expectMessage(response)
  }

  def operationsModeAndAssertResponse(
      response: OperationsModeResponse,
      handlerMockResponse: Future[Done]
  ): OperationsModeResponse = {
    when(script.executeOperationsMode()).thenReturn(handlerMockResponse)

    val probe = TestProbe[OperationsModeResponse]()
    sequencerActor ! OperationsMode(probe.ref)

    verify(script, timeout(1000)).executeOperationsMode()
    probe.expectMessage(response)
  }

  def mayBeNextAndAssertResponse(response: Option[Step]): Option[Step] = {
    val probe = TestProbe[Option[Step]]()
    sequencerActor ! MaybeNext(probe.ref)
    probe.expectMessageType[Option[Step]]
  }

  def assertSequencerState(response: SequencerState[SequencerMsg]): SequencerState[SequencerMsg] = {
    val probe = TestProbe[SequencerState[SequencerMsg]]()
    eventually {
      sequencerActor ! GetSequencerState(probe.ref)
      probe.expectMessage(response)
    }
  }

  def assertUnhandled[T >: Unhandled <: EswSequencerResponse](
      state: SequencerState[SequencerMsg],
      msg: ActorRef[T] => UnhandleableSequencerMessage
  ): Unit = {
    val probe            = TestProbe[T]()
    val sequencerMessage = msg(probe.ref)
    sequencerActor ! sequencerMessage
    probe.expectMessage(Unhandled(state.getClass.getSimpleName, sequencerMessage.getClass.getSimpleName))
  }

  def assertUnhandled[T >: Unhandled <: EswSequencerResponse](
      state: SequencerState[SequencerMsg],
      msgs: (ActorRef[T] => UnhandleableSequencerMessage)*
  ): Unit =
    msgs.foreach(assertUnhandled(state, _))

  def assertSequenceIsFinished(): Assertion = {
    val probe = TestProbe[Option[StepList]]()
    sequencerActor ! GetSequence(probe.ref)
    val stepList = probe.expectMessageType[Option[StepList]]
    val finished = stepList.get.isFinished

    if (finished) completionPromise.complete(Success(Completed(Id())))

    assertSequencerState(Idle)
    finished should ===(true)
  }

  def pullNextCommand(): PullNextResult = {
    val probe = TestProbe[PullNextResponse]()
    sequencerActor ! PullNext(probe.ref)
    probe.expectMessageType[PullNextResult]
  }

  def startPullNext(): Unit = {
    val probe = TestProbe[PullNextResponse]()
    sequencerActor ! PullNext(probe.ref)
  }

  def finishStepWithSuccess(): Unit =
    sequencerActor ! StepSuccess(deadLetter)

  def finishStepWithError(message: String): Unit =
    sequencerActor ! StepFailure(message, deadLetter)

  // this is to simulate engine pull and executing steps
  private def pullAllSteps(sequencer: ActorRef[SequencerMsg]): Unit =
    (1 to sequence.commands.size).foreach { _ =>
      pullNextCommand()
      sequencer ! StepSuccess(deadLetter)
    }

  def getSequence(): Option[StepList] = {
    val probe = TestProbe[Option[StepList]]()
    sequencerActor ! GetSequence(probe.ref)
    probe.expectMessageType[Option[StepList]]
  }

  def assertForGettingSequenceComponent(replyTo: TestProbe[AkkaLocation]): Unit = {
    replyTo.expectMessage(sequenceComponentLocation)
    verify(locationService).find(seqCompAkkaConnection)
  }
}

object SequencerTestSetup {

  import org.mockito.MockitoSugar.when

  def idle(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val testSetup = new SequencerTestSetup(sequence)
    testSetup
  }

  def loaded(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.loadSequenceAndAssertResponse(Ok)
    sequencerSetup
  }

  def running(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.loadAndStartSequenceThenAssertRunning()
    sequencerSetup.startPullNext()
    sequencerSetup
  }

  def runningWithFirstCommandComplete(
      sequence: Sequence
  )(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.loadAndStartSequenceThenAssertRunning()
    sequencerSetup.startPullNext()
    sequencerSetup.finishStepWithSuccess()
    sequencerSetup
  }

  def offline(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val testSetup = new SequencerTestSetup(sequence)
    MockitoSugar.when(testSetup.script.executeGoOffline()).thenReturn(Future.successful(Done))
    testSetup.goOfflineAndAssertResponse(Ok)
    testSetup
  }

  def finished(sequence: Sequence)(implicit system: ActorSystem[_]): (Started, SequencerTestSetup) = {
    val sequencerSetup = idle(sequence)
    import sequencerSetup._

    when { script.executeNewSequenceHandler() }.thenAnswer(Future.successful(Done))

    val probe = TestProbe[SubmitResponse]()
    sequencerActor ! SubmitSequence(sequence, probe.ref)
    Thread.sleep(100)
    pullAllStepsAndAssertSequenceIsFinished()
    val startedResponse = probe.expectMessageType[Started]
    (startedResponse, sequencerSetup)
  }
}
