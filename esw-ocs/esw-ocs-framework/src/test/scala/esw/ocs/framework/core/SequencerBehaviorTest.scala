package esw.ocs.framework.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ActorTestKitBase, TestProbe}
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.SequenceEditor.EditorResponse
import esw.ocs.framework.api.models.messages.ProcessSequenceError
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.messages.StepListError._
import esw.ocs.framework.api.models.{Sequence, Step, StepList}
import esw.ocs.framework.dsl.ScriptDsl
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class SequencerBehaviorTest extends ActorTestKitBase with BaseTestSuite with MockitoSugar {

  "SequencerBehavior" must {

    val sequencer = mock[Sequencer]
    val scriptDsl = mock[ScriptDsl]

    val sequencerActor = spawn(SequencerBehavior.behavior(sequencer, scriptDsl))

    "processSequence" in {
      val testProbe: TestProbe[Either[ProcessSequenceError, SubmitResponse]] = TestProbe()

      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))

      val processResponse: Either[ProcessSequenceError, SubmitResponse] = Right(Completed(command1.runId))

      when(sequencer.processSequence(sequence)).thenReturn(Future.successful(processResponse))

      sequencerActor ! ProcessSequence(sequence, testProbe.ref)
      testProbe.expectMessage(processResponse)
    }

    "Available" in {
      val testProbe: TestProbe[Boolean] = TestProbe()
      val availableResponse             = true

      when(sequencer.isAvailable).thenReturn(Future.successful(availableResponse))

      sequencerActor ! Available(testProbe.ref)
      testProbe.expectMessage(availableResponse)
    }

    "GetSequence" in {
      val testProbe: TestProbe[StepList] = TestProbe()

      val stepListResponse = StepList.empty
      when(sequencer.getSequence).thenReturn(Future.successful(stepListResponse))

      sequencerActor ! GetSequence(testProbe.ref)
      testProbe.expectMessage(stepListResponse)
    }

    "GetPreviousSequence" in {
      val testProbe: TestProbe[Option[StepList]] = TestProbe()

      val stepListResponse: Option[StepList] = Some(StepList.empty)
      when(sequencer.getPreviousSequence).thenReturn(Future.successful(stepListResponse))

      sequencerActor ! GetPreviousSequence(testProbe.ref)
      testProbe.expectMessage(stepListResponse)
    }

    "Add" in {
      val testProbe: TestProbe[EditorResponse[AddError]] = TestProbe()
      val command1                                       = Setup(Prefix("test"), CommandName("command-1"), None)

      val addResponse: EditorResponse[AddError] = Right(Done)
      when(sequencer.add(List(command1))).thenReturn(Future.successful(addResponse))

      sequencerActor ! Add(List(command1), testProbe.ref)
      testProbe.expectMessage(addResponse)
    }

    "Pause" in {
      val testProbe: TestProbe[EditorResponse[PauseError]] = TestProbe()

      val pauseResponse: EditorResponse[PauseError] = Right(Done)
      when(sequencer.pause).thenReturn(Future.successful(pauseResponse))

      sequencerActor ! Pause(testProbe.ref)
      testProbe.expectMessage(pauseResponse)
    }

    "Resume" in {
      val testProbe: TestProbe[EditorResponse[ResumeError]] = TestProbe()

      val resumeResponse: EditorResponse[ResumeError] = Right(Done)
      when(sequencer.resume).thenReturn(Future.successful(resumeResponse))

      sequencerActor ! Resume(testProbe.ref)
      testProbe.expectMessage(resumeResponse)
    }

    "Reset" in {
      val testProbe: TestProbe[EditorResponse[ResetError]] = TestProbe()

      val resetResponse: EditorResponse[ResetError] = Right(Done)
      when(sequencer.reset()).thenReturn(Future.successful(resetResponse))

      sequencerActor ! Reset(testProbe.ref)
      testProbe.expectMessage(resetResponse)
    }

    "Replace" in {
      val testProbe: TestProbe[EditorResponse[ReplaceError]] = TestProbe()
      val runId                                              = Id()
      val command1                                           = Setup(Prefix("test"), CommandName("command-1"), None)

      val replaceResponse: EditorResponse[ReplaceError] = Right(Done)
      when(sequencer.replace(runId, List(command1))).thenReturn(Future.successful(replaceResponse))

      sequencerActor ! Replace(runId, List(command1), testProbe.ref)
      testProbe.expectMessage(replaceResponse)
    }

    "Prepend" in {
      val testProbe: TestProbe[EditorResponse[PrependError]] = TestProbe()
      val command1                                           = Setup(Prefix("test"), CommandName("command-1"), None)

      val prependResponse: EditorResponse[PrependError] = Right(Done)
      when(sequencer.prepend(List(command1))).thenReturn(Future.successful(prependResponse))

      sequencerActor ! Prepend(List(command1), testProbe.ref)
      testProbe.expectMessage(prependResponse)
    }

    "Delete" in {
      val testProbe: TestProbe[EditorResponse[DeleteError]] = TestProbe()
      val runId                                             = Id()

      val deleteResponse: EditorResponse[DeleteError] = Right(Done)
      when(sequencer.delete(runId)).thenReturn(Future.successful(deleteResponse))

      sequencerActor ! Delete(runId, testProbe.ref)
      testProbe.expectMessage(deleteResponse)
    }

    "InsertAfter" in {
      val testProbe: TestProbe[EditorResponse[InsertError]] = TestProbe()
      val runId                                             = Id()
      val command1                                          = Setup(Prefix("test"), CommandName("command-1"), None)

      val insertResponse: EditorResponse[InsertError] = Right(Done)
      when(sequencer.insertAfter(runId, List(command1))).thenReturn(Future.successful(insertResponse))

      sequencerActor ! InsertAfter(runId, List(command1), testProbe.ref)
      testProbe.expectMessage(insertResponse)
    }

    "AddBreakpoint" in {
      val testProbe: TestProbe[EditorResponse[AddBreakpointError]] = TestProbe()
      val runId                                                    = Id()

      val addBreakpointResponse: EditorResponse[AddBreakpointError] = Right(Done)
      when(sequencer.addBreakpoint(runId)).thenReturn(Future.successful(addBreakpointResponse))

      sequencerActor ! AddBreakpoint(runId, testProbe.ref)
      testProbe.expectMessage(addBreakpointResponse)
    }

    "RemoveBreakpoint" in {
      val testProbe: TestProbe[EditorResponse[RemoveBreakpointError]] = TestProbe()
      val runId                                                       = Id()

      val removeBreakpointResponse: EditorResponse[RemoveBreakpointError] = Right(Done)
      when(sequencer.removeBreakpoint(runId)).thenReturn(Future.successful(removeBreakpointResponse))

      sequencerActor ! RemoveBreakpoint(runId, testProbe.ref)
      testProbe.expectMessage(removeBreakpointResponse)
    }

    "PullNext" in {
      val testProbe: TestProbe[Step] = TestProbe()
      val command1                   = Setup(Prefix("test"), CommandName("command-1"), None)

      val pullNextResponse = Step(command1)

      when(sequencer.pullNext()).thenReturn(Future.successful(pullNextResponse))

      sequencerActor ! PullNext(testProbe.ref)
      testProbe.expectMessage(pullNextResponse)
    }

    "MaybeNext" in {
      val testProbe: TestProbe[Option[Step]] = TestProbe()
      val command1                           = Setup(Prefix("test"), CommandName("command-1"), None)

      val mayBeNextResponse = Some(Step(command1))

      when(sequencer.mayBeNext).thenReturn(Future.successful(mayBeNextResponse))

      sequencerActor ! MaybeNext(testProbe.ref)
      testProbe.expectMessage(mayBeNextResponse)
    }

    "ReadyToExecuteNext" in {
      val testProbe: TestProbe[Done] = TestProbe()

      val ReadyToExecuteNextResponse = Done

      when(sequencer.readyToExecuteNext()).thenReturn(Future.successful(ReadyToExecuteNextResponse))

      sequencerActor ! ReadyToExecuteNext(testProbe.ref)
      testProbe.expectMessage(ReadyToExecuteNextResponse)
    }

    "UpdateFailure" in {
      val errorResponse: SubmitResponse = Error(Id(), "")

      sequencerActor ! UpdateFailure(errorResponse)

      verify(sequencer).updateFailure(errorResponse)
    }
  }
}
