package esw.ocs.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.models.SequencerState.{Idle, InProgress}
import esw.ocs.api.models.messages.SequencerMessages.{Pause, _}
import esw.ocs.api.models.messages.{LoadSequenceResponse, _}
import esw.ocs.api.models.{SequencerState, Step, StepList}
import esw.ocs.dsl.Script
import org.mockito.Mockito.when
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.{Assertion, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.Success

class SequencerTestSetup(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout) {
  import Matchers._
  import MockitoSugar._

  implicit val ec: ExecutionContext = system.executionContext

  private val componentId                 = mock[ComponentId]
  private val script                      = mock[Script]
  private val locationService             = mock[LocationService]
  private val crm: CommandResponseManager = mock[CommandResponseManager]
  private val sequencerBehavior           = new SequencerBehavior(componentId, script, locationService, crm)

  val sequencerName = s"SequencerActor${math.random()}"
  val sequencerActor: ActorRef[SequencerMsg] =
    Await.result(system.systemActorOf(sequencerBehavior.setup, sequencerName), 5.seconds)

  private val completionPromise = Promise[SubmitResponse]()
  mockCommand(sequence.runId, completionPromise.future)

  def mockCommand(id: Id, mockResponse: Future[SubmitResponse]): Unit = {
    when(crm.queryFinal(id)).thenAnswer(_ => mockResponse)
  }

  // mock all commands to return Completed submit response
  def mockAllCommands(): Unit =
    sequence.commands.foreach { command =>
      mockCommand(command.runId, Future.successful(Completed(command.runId)))
    }

  def loadSequenceAndAssertResponse(expected: LoadSequenceResponse): Unit = {
    val probe = TestProbe[LoadSequenceResponse]
    sequencerActor ! LoadSequence(sequence, probe.ref)
    probe.expectMessage(expected)
  }

  def loadAndStartSequenceThenAssertInProgress(): Assertion = {
    val probe = TestProbe[SubmitResponse]
    sequencerActor ! LoadAndStartSequence(sequence, probe.ref)

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
      val stepList = probe.expectMessageType[Option[StepList]]
      stepList.get.runId should ===(sequence.runId)
    }

    pullAllSteps()
    eventually(assertSequenceIsFinished())
  }

  def assertCurrentSequence(expected: Option[StepList]): Unit = {
    val probe = TestProbe[Option[StepList]]
    sequencerActor ! GetSequence(probe.ref)
    probe.expectMessage(expected)
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

  def goOfflineAndAssertResponse(response: OkOrUnhandledResponse, handlerMockResponse: Future[Done]): OkOrUnhandledResponse = {
    when(script.executeGoOffline()).thenReturn(handlerMockResponse)

    val probe = TestProbe[OkOrUnhandledResponse]
    sequencerActor ! GoOffline(probe.ref)
    probe.expectMessage(response)
  }

  def goOnlineAndAssertResponse(response: GoOnlineResponse, handlerMockResponse: Future[Done]): GoOnlineResponse = {
    when(script.executeGoOnline()).thenReturn(handlerMockResponse)

    val probe = TestProbe[GoOnlineResponse]
    sequencerActor ! GoOnline(probe.ref)
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
    probe.expectMessage(Unhandled(state, sequencerMessage.getClass.getSimpleName))
  }

  def assertUnhandled[T >: Unhandled <: EswSequencerResponse](
      state: SequencerState[SequencerMsg],
      msgs: (ActorRef[T] => UnhandleableSequencerMessage)*
  ): Unit =
    msgs.foreach(assertUnhandled(state, _))

  private def assertSequenceIsFinished(): Assertion = {
    val probe = TestProbe[Option[StepList]]
    sequencerActor ! GetSequence(probe.ref)
    val stepList = probe.expectMessageType[Option[StepList]]
    val finished = stepList.get.isFinished

    if (finished) completionPromise.complete(Success(Completed(sequence.runId)))

    finished should ===(true)
  }

  def pullNextCommand(): PullNextResult = {
    val probe = TestProbe[PullNextResponse]
    sequencerActor ! PullNext(probe.ref)
    probe.expectMessageType[PullNextResult]
  }

  // this is to simulate engine pull and executing steps
  private def pullAllSteps(): Seq[PullNextResult] = {
    mockAllCommands()
    (1 to sequence.commands.size).map(_ => pullNextCommand())
  }

  def getSequence(): Option[StepList] = {
    val probe = TestProbe[Option[StepList]]
    sequencerActor ! GetSequence(probe.ref)
    probe.expectMessageType[Option[StepList]]
  }
}

object SequencerTestSetup {

  def idle(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout): SequencerTestSetup = {
    val testSetup = new SequencerTestSetup(sequence)
    testSetup
  }

  def loaded(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.loadSequenceAndAssertResponse(Ok)
    sequencerSetup
  }

  def inProgress(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout): SequencerTestSetup = {
    val sequencerSetup = idle(sequence)
    sequencerSetup.mockCommand(sequence.commands.head.runId, Promise[SubmitResponse].future)
    sequencerSetup.loadAndStartSequenceThenAssertInProgress()
    sequencerSetup.pullNextCommand()
    sequencerSetup
  }

  def offline(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout): SequencerTestSetup = {
    val testSetup = new SequencerTestSetup(sequence)
    testSetup.goOfflineAndAssertResponse(Ok, Future.successful(Done))
    testSetup
  }

  def finished(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout): SequencerTestSetup = {
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._
    val probe = TestProbe[SubmitResponse]
    sequencerActor ! LoadAndStartSequence(sequence, probe.ref)
    pullAllStepsAndAssertSequenceIsFinished()
    probe.expectMessage(Completed(sequence.runId))
    sequencerSetup
  }
}
