package esw.ocs.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ActorTestKitBase, TestProbe}
import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.BaseTestSuite
import esw.ocs.api.SequenceEditor.EditorResponse
import esw.ocs.api.models.messages.SequencerMsg
import esw.ocs.api.models.messages.SequencerMsg._
import esw.ocs.api.models.messages.error.ProcessSequenceError
import esw.ocs.api.models.messages.error.StepListError._
import esw.ocs.api.models.{Sequence, Step, StepList}
import esw.ocs.dsl.ScriptDsl
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class SequencerBehaviorTest extends ActorTestKitBase with BaseTestSuite with MockitoSugar {
  private val sequencer = mock[Sequencer]
  private val scriptDsl = mock[ScriptDsl]

  private val sequencerActor = spawn(SequencerBehavior.behavior(sequencer, scriptDsl))

  "ProcessSequence" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
    val sequence = Sequence(Id(), Seq(command1))

    runTest[Either[ProcessSequenceError, SubmitResponse]](
      mockFunction = sequencer.processSequence(sequence),
      mockResponse = Right(Completed(command1.runId)),
      testMsg = ProcessSequence(sequence, _)
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
    runTest[Option[StepList]](
      mockFunction = sequencer.getPreviousSequence,
      mockResponse = Some(StepList.empty),
      testMsg = GetPreviousSequence
    )
  }

  "Add" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTest[EditorResponse[AddError]](
      mockFunction = sequencer.add(List(command1)),
      mockResponse = Right(Done),
      testMsg = Add(List(command1), _)
    )
  }

  "Pause" in {
    runTest[EditorResponse[PauseError]](
      mockFunction = sequencer.pause,
      mockResponse = Right(Done),
      testMsg = Pause
    )
  }

  "Resume" in {
    runTest[EditorResponse[ResumeError]](
      mockFunction = sequencer.resume,
      mockResponse = Right(Done),
      testMsg = Resume
    )
  }

  "Reset" in {
    runTest[EditorResponse[ResetError]](
      mockFunction = sequencer.reset(),
      mockResponse = Right(Done),
      testMsg = Reset
    )
  }

  "Replace" in {
    val runId    = Id()
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTest[EditorResponse[ReplaceError]](
      mockFunction = sequencer.replace(runId, List(command1)),
      mockResponse = Right(Done),
      testMsg = Replace(runId, List(command1), _)
    )
  }

  "Prepend" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTest[EditorResponse[PrependError]](
      mockFunction = sequencer.prepend(List(command1)),
      mockResponse = Right(Done),
      testMsg = Prepend(List(command1), _)
    )

  }

  "Delete" in {
    val runId = Id()

    runTest[EditorResponse[DeleteError]](
      mockFunction = sequencer.delete(runId),
      mockResponse = Right(Done),
      testMsg = Delete(runId, _)
    )
  }

  "InsertAfter" in {
    val runId    = Id()
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)

    runTest[EditorResponse[InsertError]](
      mockFunction = sequencer.insertAfter(runId, List(command1)),
      mockResponse = Right(Done),
      testMsg = InsertAfter(runId, List(command1), _)
    )
  }

  "AddBreakpoint" in {
    val runId = Id()

    runTest[EditorResponse[AddBreakpointError]](
      mockFunction = sequencer.addBreakpoint(runId),
      mockResponse = Right(Done),
      testMsg = AddBreakpoint(runId, _)
    )
  }

  "RemoveBreakpoint" in {
    val runId = Id()

    runTest[EditorResponse[RemoveBreakpointError]](
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

  private def runTest[R](mockFunction: => Future[R], mockResponse: R, testMsg: ActorRef[R] => SequencerMsg): Unit = {
    val testProbe: TestProbe[R] = TestProbe()

    when(mockFunction).thenReturn(Future.successful(mockResponse))

    sequencerActor ! testMsg(testProbe.ref)
    testProbe.expectMessage(mockResponse)

  }
}
