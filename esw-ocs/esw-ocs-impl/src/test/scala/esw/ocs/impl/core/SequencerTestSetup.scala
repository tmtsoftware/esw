package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequenceAndWait
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.dsl.script.JScriptDsl
import esw.ocs.impl.messages.SequencerMessages.{Pause, _}
import esw.ocs.impl.messages.SequencerState
import esw.ocs.impl.messages.SequencerState.{Idle, InProgress}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.Eventually._
import org.scalatest.{Assertion, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class SequencerTestSetup(sequence: Sequence)(implicit system: ActorSystem[_]) {
  import Matchers._
  import MockitoSugar._

  implicit private val patienceConfig: PatienceConfig = PatienceConfig(5.seconds)
  implicit val ec: ExecutionContext                   = system.executionContext

  private val componentId                                      = mock[ComponentId]
  private val script                                           = mock[JScriptDsl]
  private val locationService                                  = mock[LocationService]
  private def mockShutdownHttpService: () => Future[Done.type] = () => Future { Done }
  private val sequencerBehavior                                = new SequencerBehavior(componentId, script, locationService, mockShutdownHttpService)

  val sequencerName                          = s"SequencerActor${math.random()}"
  val sequencerActor: ActorRef[SequencerMsg] = system.systemActorOf(sequencerBehavior.setup, sequencerName)

  private def deadletter = system.deadLetters

  private val completionPromise = Promise[SubmitResponse]()

  def loadSequenceAndAssertResponse(expected: OkOrUnhandledResponse): Unit = {
    val probe = TestProbe[OkOrUnhandledResponse]
    sequencerActor ! LoadSequence(sequence, probe.ref)
    probe.expectMessage(expected)
  }

  def loadAndStartSequenceThenAssertInProgress(): Assertion = {
    val probe = TestProbe[SequencerSubmitResponse]
    sequencerActor ! SubmitSequence(sequence, probe.ref)

    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]
    eventually {
      sequencerActor ! GetSequence(p.ref)
      val stepList = p.expectMessageType[Option[StepList]]
      stepList.isDefined shouldBe true
    }
  }

  def pullAllStepsAndAssertSequenceIsFinished(): Assertion = {
    eventually {
      val probe = TestProbe[Option[StepList]]
      sequencerActor ! GetSequence(probe.ref)
      probe.expectMessageType[Option[StepList]]
    }

    pullAllSteps(sequencerActor)
    eventually(assertSequenceIsFinished())
  }

  def assertCurrentSequence(expected: Option[StepList]): Unit = {
    val probe = TestProbe[Option[StepList]]
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
    val probe = TestProbe[Ok.type]
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
    val probe                          = TestProbe[OkOrUnhandledResponse]
    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]

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
      case InProgress                      => stepList.get.nextPending shouldBe None
      case x: SequencerState[SequencerMsg] => assert(false, s"$x is not valid state after AbortSequence")
    }
    eventually(verify(script).executeAbort())
    probe
  }

  def stopAndAssertResponse(
      response: OkOrUnhandledResponse,
      expectedState: SequencerState[SequencerMsg]
  ): TestProbe[OkOrUnhandledResponse] = {
    val probe                          = TestProbe[OkOrUnhandledResponse]
    val p: TestProbe[Option[StepList]] = TestProbe[Option[StepList]]

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
      case InProgress                      => stepList shouldNot be(None)
      case x: SequencerState[SequencerMsg] => assert(false, s"$x is not valid state after AbortSequence")
    }
    eventually(verify(script).executeStop())
    probe
  }

  def pauseAndAssertResponse(response: PauseResponse): PauseResponse = {
    val probe = TestProbe[PauseResponse]
    sequencerActor ! Pause(probe.ref)
    probe.expectMessage(response)
  }

  def resumeAndAssertResponse(response: OkOrUnhandledResponse): OkOrUnhandledResponse = {
    val probe = TestProbe[OkOrUnhandledResponse]
    sequencerActor ! Resume(probe.ref)
    probe.expectMessage(response)
  }

  def resetAndAssertResponse(response: OkOrUnhandledResponse): OkOrUnhandledResponse = {
    val probe = TestProbe[OkOrUnhandledResponse]
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

  def goOfflineAndAssertResponse(response: GoOfflineResponse, handlerMockResponse: Future[Done]): GoOfflineResponse = {
    when(script.executeGoOffline()).thenReturn(handlerMockResponse)

    val probe = TestProbe[GoOfflineResponse]
    sequencerActor ! GoOffline(probe.ref)
    probe.expectMessage(response)
  }

  def goOnlineAndAssertResponse(response: GoOnlineResponse, handlerMockResponse: Future[Done]): GoOnlineResponse = {
    when(script.executeGoOnline()).thenReturn(handlerMockResponse)

    val probe = TestProbe[GoOnlineResponse]
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

    val probe = TestProbe[DiagnosticModeResponse]
    sequencerActor ! DiagnosticMode(startTime, hint, probe.ref)

    eventually(verify(script).executeDiagnosticMode(startTime, hint))
    probe.expectMessage(response)
  }

  def operationsModeAndAssertResponse(
      response: OperationsModeResponse,
      handlerMockResponse: Future[Done]
  ): OperationsModeResponse = {
    when(script.executeOperationsMode()).thenReturn(handlerMockResponse)

    val probe = TestProbe[OperationsModeResponse]
    sequencerActor ! OperationsMode(probe.ref)

    eventually(verify(script).executeOperationsMode())
    probe.expectMessage(response)
  }

  def mayBeNextAndAssertResponse(response: Option[Step]): Option[Step] = {
    val probe = TestProbe[Option[Step]]
    sequencerActor ! MaybeNext(probe.ref)
    probe.expectMessageType[Option[Step]]
  }

  def assertSequencerState(response: SequencerState[SequencerMsg]): SequencerState[SequencerMsg] = {
    val probe = TestProbe[SequencerState[SequencerMsg]]
    eventually {
      sequencerActor ! GetSequencerState(probe.ref)
      probe.expectMessage(response)
    }
  }

  def assertUnhandled[T >: Unhandled <: EswSequencerResponse](
      state: SequencerState[SequencerMsg],
      msg: ActorRef[T] => UnhandleableSequencerMessage
  ): Unit = {
    val probe            = TestProbe[T]
    val sequencerMessage = msg(probe.ref)
    sequencerActor ! sequencerMessage
    probe.expectMessage(Unhandled(state.entryName, sequencerMessage.getClass.getSimpleName))
  }

  def assertUnhandled[T >: Unhandled <: EswSequencerResponse](
      state: SequencerState[SequencerMsg],
      msgs: (ActorRef[T] => UnhandleableSequencerMessage)*
  ): Unit =
    msgs.foreach(assertUnhandled(state, _))

  def assertSequenceIsFinished(): Assertion = {
    val probe = TestProbe[Option[StepList]]
    sequencerActor ! GetSequence(probe.ref)
    val stepList = probe.expectMessageType[Option[StepList]]
    val finished = stepList.get.isFinished

    if (finished) completionPromise.complete(Success(Completed(Id())))

    finished should ===(true)
  }

  def pullNextCommand(): PullNextResult = {
    val probe = TestProbe[PullNextResponse]
    sequencerActor ! PullNext(probe.ref)
    probe.expectMessageType[PullNextResult]
  }

  def startPullNext(): Unit = {
    val probe = TestProbe[PullNextResponse]
    sequencerActor ! PullNext(probe.ref)
  }

  def finishStepWithSuccess(): Unit =
    sequencerActor ! StepSuccess(deadletter)

  def finishStepWithError(message: String): Unit =
    sequencerActor ! StepFailure(message, deadletter)

  // this is to simulate engine pull and executing steps
  private def pullAllSteps(sequencer: ActorRef[SequencerMsg]): Unit = {
    (1 to sequence.commands.size).foreach(_ => {
      pullNextCommand()
      sequencer ! StepSuccess(deadletter)
    })
  }

  def getSequence(): Option[StepList] = {
    val probe = TestProbe[Option[StepList]]
    sequencerActor ! GetSequence(probe.ref)
    probe.expectMessageType[Option[StepList]]
  }
}

object SequencerTestSetup {

  def idle(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val testSetup = new SequencerTestSetup(sequence)
    testSetup
  }

  def loaded(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.loadSequenceAndAssertResponse(Ok)
    sequencerSetup
  }

  def inProgress(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.loadAndStartSequenceThenAssertInProgress()
    sequencerSetup.startPullNext()
    sequencerSetup
  }

  def inProgressWithFirstCommandComplete(
      sequence: Sequence
  )(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.loadAndStartSequenceThenAssertInProgress()
    sequencerSetup.startPullNext()
    sequencerSetup.finishStepWithSuccess()
    sequencerSetup
  }

  def offline(sequence: Sequence)(implicit system: ActorSystem[_]): SequencerTestSetup = {
    val testSetup = new SequencerTestSetup(sequence)
    testSetup.goOfflineAndAssertResponse(Ok, Future.successful(Done))
    testSetup
  }

  def finished(sequence: Sequence)(implicit system: ActorSystem[_]): (Completed, SequencerTestSetup) = {
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._
    val probe = TestProbe[SubmitResponse]
    sequencerActor ! SubmitSequenceAndWait(sequence, probe.ref)
    pullAllStepsAndAssertSequenceIsFinished()
    val completedResponse = probe.expectMessageType[Completed]
    (completedResponse, sequencerSetup)
  }
}
