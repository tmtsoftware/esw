package esw.ocs.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.scaladsl.LocationService
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.Sequence
import esw.ocs.api.models.SequencerState
import esw.ocs.api.models.messages.SequencerMessages.{Pause, _}
import esw.ocs.api.models.messages._
import esw.ocs.dsl.Script
import org.mockito.Mockito.when
import org.scalatest.concurrent.Eventually
import org.scalatest.{Assertion, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.Success

class SequencerTestSetup(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout)
    extends MockitoSugar
    with Matchers {
  import Eventually._

  val crm: CommandResponseManager   = mock[CommandResponseManager]
  implicit val ec: ExecutionContext = system.executionContext

  private val completionPromise = Promise[SubmitResponse]()
  when(crm.queryFinal(sequence.runId)).thenReturn(completionPromise.future)

  private val script            = mock[Script]
  private val locationService   = mock[LocationService]
  private val componentId       = ComponentId("sequencer1", ComponentType.Sequencer)
  private val sequencerBehavior = new SequencerBehavior(componentId, script, locationService, crm)
  val sequencerActor: ActorRef[SequencerMsg] =
    Await.result(system.systemActorOf(sequencerBehavior.setup, s"SequencerActor${math.random()}"), 5.seconds)

  def pullAndAssertSequenceCompletion(): Assertion = {
    eventually {
      val probe = TestProbe[StepListResponse]
      sequencerActor ! GetSequence(probe.ref)
      val result = probe.expectMessageType[StepListResult]
      result.stepList.get.runId should ===(sequence.runId)
    }

    pullAllSteps()
    eventually(assertSequenceCompletion())
  }

  def mockAllCommandResponses(): Unit = sequence.commands.foreach { command =>
    when(crm.queryFinal(command.runId)).thenAnswer(_ => Future.successful(Completed(command.runId)))
  }

  def pullAllSteps(): Seq[PullNextResult] =
    (1 until sequence.commands.size).map { _ =>
      val probe = TestProbe[PullNextResponse]
      sequencerActor ! PullNext(probe.ref)
      probe.expectMessageType[PullNextResult]
    }

  def assertSequenceCompletion(): Assertion = {
    val probe = TestProbe[StepListResponse]
    sequencerActor ! GetSequence(probe.ref)
    val result   = probe.expectMessageType[StepListResult]
    val finished = result.stepList.get.isFinished

    if (finished) completionPromise.complete(Success(Completed(sequence.runId)))

    finished should ===(true)
  }

  private def assertStepListResponse(expected: StepListResponse, msg: ActorRef[StepListResponse] => SequencerMsg): Unit = {
    val probe = TestProbe[StepListResponse]
    sequencerActor ! msg(probe.ref)
    probe.expectMessage(expected)
  }

  def assertCurrentSequence(expected: StepListResponse): Unit = assertStepListResponse(expected, GetSequence)

  def assertPreviousSequence(expected: StepListResponse): Unit = assertStepListResponse(expected, GetPreviousSequence)

  def assertSequencerIsLoaded(expected: LoadSequenceResponse): Unit = {
    val probe = TestProbe[LoadSequenceResponse]
    sequencerActor ! LoadSequence(sequence, probe.ref)
    probe.expectMessage(expected)
  }

  def assertSequencerIsInProgress(): TestProbe[SubmitResponse] = {
    val submitResponsePromise = Promise[SubmitResponse]
    when(crm.queryFinal(sequence.commands.head.runId)).thenReturn(submitResponsePromise.future)

    val probe = TestProbe[SubmitResponse]
    sequencerActor ! LoadAndStartSequence(sequence, probe.ref)
    val p: TestProbe[StepListResponse] = TestProbe[StepListResponse]

    eventually {
      sequencerActor ! GetSequence(p.ref)
      val result = p.expectMessageType[StepListResult]
      result.stepList.isDefined shouldBe true
    }
    probe
  }

  def assertSequenceIsAborted(): TestProbe[OkOrUnhandledResponse] = {
    val probe = TestProbe[OkOrUnhandledResponse]
    when(script.executeAbort()).thenReturn(Future.successful(Done))
    sequencerActor ! AbortSequence(probe.ref)
    val p: TestProbe[StepListResponse] = TestProbe[StepListResponse]

    eventually {
      sequencerActor ! GetSequence(p.ref)
      val result = p.expectMessageType[StepListResult]
      result.stepList.get.nextPending shouldBe None
      probe.expectMessage(Ok)
    }
    probe
  }

  def assertSequenceIsPaused(): Ok.type = {
    val probe = TestProbe[PauseResponse]
    sequencerActor ! Pause(probe.ref)
    probe.expectMessage(Ok)
  }

  def assertSequenceIsResumed(): Ok.type = {
    val probe = TestProbe[OkOrUnhandledResponse]
    sequencerActor ! Resume(probe.ref)
    probe.expectMessage(Ok)
  }

  def assertUnhandled[T >: Unhandled <: EswSequencerResponse](
      state: SequencerState,
      msg: ActorRef[T] => EswSequencerMessage
  ): Unit = {
    val probe            = TestProbe[T]
    val sequencerMessage = msg(probe.ref)
    sequencerActor ! sequencerMessage
    probe.expectMessage(Unhandled(state, sequencerMessage.getClass.getSimpleName))
  }

  def assertUnhandled[T >: Unhandled <: EswSequencerResponse](
      state: SequencerState,
      msgs: (ActorRef[T] => EswSequencerMessage)*
  ): Unit =
    msgs.foreach(assertUnhandled(state, _))

}
