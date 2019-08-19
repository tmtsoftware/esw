package esw.ocs.core

import java.util.concurrent.CountDownLatch

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.scaladsl.LocationService
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, Sequence}
import esw.ocs.api.models.SequencerState
import esw.ocs.api.models.messages.SequencerMessages.{EswSequencerMessage, GetPreviousSequence, GetSequence, LoadSequence}
import esw.ocs.api.models.messages._
import esw.ocs.dsl.Script
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.Success

class SequencerTestSetup(sequence: Sequence)(implicit system: ActorSystem[_], timeout: Timeout)
    extends MockitoSugar
    with Eventually
    with Matchers {

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

  def queryResponse(submitResponse: SubmitResponse, latch: CountDownLatch) = Future {
    latch.countDown()
    if (latch.getCount == 0) completionPromise.complete(Success(CommandResponse.withRunId(sequence.runId, submitResponse)))
    submitResponse
  }

  private def assertStepListResponse(expected: StepListResponse, msg: ActorRef[StepListResponse] => SequencerMsg): Unit = {
    val probe = TestProbe[StepListResponse]
    sequencerActor ! msg(probe.ref)
    probe.expectMessage(expected)
  }

  def assertCurrentSequence(expected: StepListResponse): Unit =
    assertStepListResponse(expected, GetSequence)

  def assertPreviousSequence(expected: StepListResponse): Unit =
    assertStepListResponse(expected, GetPreviousSequence)

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
