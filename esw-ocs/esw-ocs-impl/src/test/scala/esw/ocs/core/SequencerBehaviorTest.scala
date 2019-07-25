package esw.ocs.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.scaladsl.LocationService
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.error.EditorError
import esw.ocs.api.models.messages.{EditorResponse, StepListResponse}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.dsl.{Script, ScriptDsl}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class SequencerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite with MockitoSugar {
  private var sequencer: Sequencer                   = _
  private var scriptDsl: ScriptDsl                   = _
  private var sequencerActor: ActorRef[SequencerMsg] = _
  private var locationService: LocationService       = _

  private val componentId = ComponentId("sequencer1", ComponentType.Sequencer)

  override protected def beforeEach(): Unit = {
    sequencer = mock[Sequencer]
    scriptDsl = mock[Script]
    locationService = mock[LocationService]
    sequencerActor = spawn(SequencerBehavior.behavior(sequencer, scriptDsl))
  }

  "LoadAndStartSequence" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
    val sequence = Sequence(Id(), Seq(command1))

    runTest[ProcessSequenceResponse](
      mockFunction = sequencer.loadAndStartSequence(sequence),
      mockResponse = ProcessSequenceResponse(Right(Completed(command1.runId))),
      testMsg = LoadAndStartSequence(sequence, _)
    )
  }

  "Available" in {
    runTest[Boolean](
      mockFunction = sequencer.isAvailable,
      mockResponse = true,
      testMsg = Available
    )
  }

  "GetSequence" in {
    runTest[StepList](
      mockFunction = sequencer.getSequence,
      mockResponse = StepList.empty,
      testMsg = GetSequence
    )
  }

  "GetPreviousSequence" in {
    runTestFor[Option[StepList], StepListResponse](
      StepListResponse,
      mockFunction = sequencer.getPreviousSequence,
      mockResponse = Some(StepList.empty),
      testMsg = GetPreviousSequence
    )
  }

  "Add" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.add(List(command1)),
      mockResponse = Right(Done),
      testMsg = Add(List(command1), _)
    )
  }

  "Pause" in {
    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.pause,
      mockResponse = Right(Done),
      testMsg = Pause
    )
  }

  "Resume" in {
    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.resume,
      mockResponse = Right(Done),
      testMsg = Resume
    )
  }

  "Reset" in {
    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.reset(),
      mockResponse = Right(Done),
      testMsg = Reset
    )
  }

  "Replace" in {
    val runId    = Id()
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.replace(runId, List(command1)),
      mockResponse = Right(Done),
      testMsg = Replace(runId, List(command1), _)
    )
  }

  "Prepend" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.prepend(List(command1)),
      mockResponse = Right(Done),
      testMsg = Prepend(List(command1), _)
    )

  }

  "Delete" in {
    val runId = Id()

    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.delete(runId),
      mockResponse = Right(Done),
      testMsg = Delete(runId, _)
    )
  }

  "InsertAfter" in {
    val runId    = Id()
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.insertAfter(runId, List(command1)),
      mockResponse = Right(Done),
      testMsg = InsertAfter(runId, List(command1), _)
    )
  }

  "AddBreakpoint" in {
    val runId = Id()

    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.addBreakpoint(runId),
      mockResponse = Right(Done),
      testMsg = AddBreakpoint(runId, _)
    )
  }

  "RemoveBreakpoint" in {
    val runId = Id()

    runTestFor[Either[EditorError, Done], EditorResponse](
      EditorResponse,
      mockFunction = sequencer.removeBreakpoint(runId),
      mockResponse = Right(Done),
      testMsg = RemoveBreakpoint(runId, _)
    )
  }

  "PullNext" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTest[Step](
      mockFunction = sequencer.pullNext(),
      mockResponse = Step(command1),
      testMsg = PullNext
    )
  }

  "MaybeNext" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTest[Option[Step]](
      mockFunction = sequencer.mayBeNext,
      mockResponse = Some(Step(command1)),
      testMsg = MaybeNext
    )
  }

  "ReadyToExecuteNext" in {
    runTest[Done](
      mockFunction = sequencer.readyToExecuteNext(),
      mockResponse = Done,
      testMsg = ReadyToExecuteNext
    )
  }

  "UpdateFailure" in {
    val errorResponse: SubmitResponse = Error(Id(), "")

    sequencerActor ! UpdateFailure(errorResponse)

    verify(sequencer).updateFailure(errorResponse)
  }

  private def runTest[R](mockFunction: => Future[R], mockResponse: R, testMsg: ActorRef[R] => SequencerMsg): Unit =
    runTestFor[R, R](identity, mockFunction, mockResponse, testMsg)

  private def runTestFor[R, T](
      factory: R => T,
      mockFunction: => Future[R],
      mockResponse: R,
      testMsg: ActorRef[T] => SequencerMsg
  ): T = {
    val testProbe: TestProbe[T] = TestProbe()

    when(mockFunction).thenReturn(Future.successful(mockResponse))

    sequencerActor ! testMsg(testProbe.ref)
    testProbe.expectMessage(factory(mockResponse))
  }
}
