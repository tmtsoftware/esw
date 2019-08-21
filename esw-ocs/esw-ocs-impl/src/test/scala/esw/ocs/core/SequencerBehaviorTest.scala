package esw.ocs.core

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.util.Timeout
import csw.command.client.messages.sequencer.LoadAndStartSequence
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.SequencerState.{Idle, InProgress, Loaded}
import esw.ocs.api.models.StepStatus.{InFlight, Pending}
import esw.ocs.api.models.messages.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages._
import esw.ocs.api.models.{Step, StepList, StepStatus}

import scala.concurrent.{Future, Promise}

class SequencerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val command4 = Setup(Prefix("esw.test"), CommandName("command-4"), None)
  private val sequence = Sequence(Id(), Seq(command1, command2))

  private implicit val timeoutDuration: Timeout = timeout

  def Finished(id: Id): StepStatus = StepStatus.Finished.Success(Completed(id))

  "LoadSequence" must {
    "load the given sequence in idle state" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadSequenceAndAssertResponse(Ok)
    }

    "fail when given sequence contains duplicate Ids" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))
      val sequencerSetup  = SequencerTestSetup.idle(invalidSequence)
      import sequencerSetup._

      loadSequenceAndAssertResponse(DuplicateIdsFound)
    }
  }

  "StartSequence" must {
    "start executing a sequence when sequencer is loaded" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = createTestProbe[SequenceResponse]
      sequencerActor ! StartSequence(probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(SequenceResult(Completed(sequence.runId)))
    }
  }

  "LoadAndStartSequence" must {
    "load and process sequence in idle state" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val probe = createTestProbe[SubmitResponse]
      sequencerActor ! LoadAndStartSequence(sequence, probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(Completed(sequence.runId))
    }

    "fail when given sequence contains duplicate Ids" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))

      val sequencerSetup = SequencerTestSetup.idle(invalidSequence)
      import sequencerSetup._

      val probe = createTestProbe[SubmitResponse]
      sequencerActor ! LoadAndStartSequence(invalidSequence, probe.ref)
      probe.expectMessage(Error(invalidSequence.runId, DuplicateIdsFound.msg))
    }

    //todo: Add test for sequence failure
  }

  "GetSequence" must {
    val sequencerSetup = SequencerTestSetup.idle(sequence)
    import sequencerSetup._

    "return None when in Idle state | ESW-157" in {
      assertCurrentSequence(StepListResult(None))
    }

    "return sequence when in Loaded state | ESW-157" in {
      loadSequenceAndAssertResponse(Ok)
      assertCurrentSequence(StepListResult(StepList(sequence).toOption))
    }
  }

  "Add" must {
    "add commands when sequence is loaded | ESW-114" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Add(List(command3), probe.ref)
      probe.expectMessage(Ok)

      val updatedSequence = sequence.copy(commands = Seq(command1, command2, command3))
      assertCurrentSequence(StepListResult(StepList(updatedSequence).toOption))
    }

    "add commands when sequence is in progress | ESW-114" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Add(List(command3), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        StepListResult(
          Some(
            StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2), Step(command3)))
          )
        )
      )
    }
  }

  "Prepend" must {
    "add steps before first pending step in Loaded state | ESW-113" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Prepend(List(command3), probe.ref)
      probe.expectMessage(Ok)

      val updatedSequence = sequence.copy(commands = Seq(command3, command1, command2))
      assertCurrentSequence(StepListResult(StepList(updatedSequence).toOption))
    }

    "add steps before first pending step in InProgress state | ESW-113" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Prepend(List(command3), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        StepListResult(
          Some(
            StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command3), Step(command2)))
          )
        )
      )
    }

  }

  "Pause" must {
    "pause sequencer when it is InProgress | ESW-104" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val beforePauseStepList = Some(
        StepList(
          sequence.runId,
          List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
        )
      )

      assertCurrentSequence(StepListResult(beforePauseStepList))

      pauseAndAssertResponse(Ok)

      val afterPauseStepList = Some(
        StepList(
          sequence.runId,
          List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = true))
        )
      )

      assertCurrentSequence(StepListResult(afterPauseStepList))
    }

    "pause sequencer when it is in Loaded state | ESW-104" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val beforePauseStepList = StepList(
        sequence.runId,
        List(Step(command1, Pending, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
      )
      assertCurrentSequence(StepListResult(Some(beforePauseStepList)))

      pauseAndAssertResponse(Ok)

      val afterPauseStepList = StepList(
        sequence.runId,
        List(Step(command1, Pending, hasBreakpoint = true), Step(command2, Pending, hasBreakpoint = false))
      )

      assertCurrentSequence(StepListResult(Some(afterPauseStepList)))
    }
  }

  "Resume" must {
    "resume a paused sequence when sequencer is loaded | ESW-105" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._
      val expectedPausedSteps = List(
        Step(command1, Pending, hasBreakpoint = true),
        Step(command2, Pending, hasBreakpoint = false)
      )

      val expectedPausedSequence = StepListResult(Some(StepList(sequence.runId, expectedPausedSteps)))

      val expectedResumedSteps = List(
        Step(command1, Pending, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )

      val expectedResumedSequence = StepListResult(Some(StepList(sequence.runId, expectedResumedSteps)))

      pauseAndAssertResponse(Ok)
      assertCurrentSequence(expectedPausedSequence)
      resumeAndAssertResponse(Ok)
      assertCurrentSequence(expectedResumedSequence)
    }

    "resume a paused sequence when sequencer is InProgress | ESW-105" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedPausedSteps = List(
        Step(command1, InFlight, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = true)
      )
      val expectedPausedSequence = StepListResult(Some(StepList(sequence.runId, expectedPausedSteps)))

      val expectedResumedSteps = List(
        Step(command1, InFlight, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )
      val expectedResumedSequence = StepListResult(Some(StepList(sequence.runId, expectedResumedSteps)))

      pauseAndAssertResponse(Ok)
      assertCurrentSequence(expectedPausedSequence)
      resumeAndAssertResponse(Ok)
      assertCurrentSequence(expectedResumedSequence)
    }
  }

  "Replace" must {
    "replace steps when sequencer is Loaded | ESW-108" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val expectedSteps = List(
        Step(command3, Pending, hasBreakpoint = false),
        Step(command4, Pending, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )

      val expectedSequence = StepListResult(Some(StepList(sequence.runId, expectedSteps)))

      replaceAndAssertResponse(command1.runId, List(command3, command4), Ok)
      assertCurrentSequence(expectedSequence)
    }

    "fail if invalid command id is provided in Loaded state | ESW-108" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._
      val invalidId = Id()
      replaceAndAssertResponse(invalidId, List(command3, command4), IdDoesNotExist(invalidId))
    }

    "fail if finished step is tried to be replaced in InProgress state| ESW-108" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      mockCommand(command2.runId, Future.successful(Completed(command2.runId)))
      pullNextCommand()
      eventually(getSequence().stepList.get.steps(1).isFinished should ===(true))

      replaceAndAssertResponse(command1.runId, List(command3, command4), CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail if inflight step is tried to be replaced in InProgress state | ESW-108" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      eventually(getSequence().stepList.get.steps.head.isInFlight should ===(true))

      replaceAndAssertResponse(command1.runId, List(command3, command4), CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "Delete" must {
    "delete steps when sequencer is Loaded | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, Pending, hasBreakpoint = false))
      val expectedSequence = StepListResult(Some(StepList(sequence.runId, expectedSteps)))

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(command2.runId, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }

    "delete steps when sequencer is InProgress | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, InFlight, hasBreakpoint = false))
      val expectedSequence = StepListResult(Some(StepList(sequence.runId, expectedSteps)))

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(command2.runId, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }
  }

  "InsertAfter" must {
    "insert steps after provided id when sequencer is in Loaded state | ESW-111" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val cmdsToInsert = List(command3, command4)

      val expectedSequenceAfterInsertion =
        StepListResult(
          Some(
            StepList(
              sequence.runId,
              List(Step(command1), Step(command3), Step(command4), Step(command2))
            )
          )
        )

      val insertResProbe = TestProbe[GenericResponse]()
      sequencerActor ! InsertAfter(command1.runId, cmdsToInsert, insertResProbe.ref)
      insertResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequenceAfterInsertion)
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep when trying to insert before a InFlight step in InProgress state | ESW-111" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      // make sure command2 is in InFlight status
      mockCommand(command2.runId, Promise[SubmitResponse].future)
      pullNextCommand()
      val stepListResult = getSequence()
      stepListResult.stepList.get.steps.forall(_.isInFlight) should ===(true)

      val cmdsToInsert   = List(command3, command4)
      val insertResProbe = TestProbe[GenericResponse]()
      sequencerActor ! InsertAfter(command1.runId, cmdsToInsert, insertResProbe.ref)
      insertResProbe.expectMessage(CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "AddBreakpoint and RemoveBreakpoint" must {
    "add and delete breakpoint to/from provided id when step status is Pending and sequencer is in Loaded state | ESW-106, ESW-107" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val expectedSequenceAfterAddingBreakpoint =
        StepListResult(Some(StepList(sequence.runId, List(Step(command1), Step(command2).copy(hasBreakpoint = true)))))

      addBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakPoint =
        StepListResult(Some(StepList(sequence.runId, List(Step(command1), Step(command2).copy(hasBreakpoint = false)))))

      removeBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterRemovingBreakPoint)
    }

    "add and delete breakpoint to/from provided id when step status is Pending and sequencer is in InProgress state | ESW-106, ESW-107" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSequenceAfterAddingBreakpoint =
        StepListResult(
          Some(StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = true))))
        )

      addBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakpoint =
        StepListResult(
          Some(StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = false))))
        )

      removeBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterRemovingBreakpoint)
    }
  }

  "AbortSequence" must {
    "abort the given sequence in Loaded state | ESW-155, ESW-137" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      abortSequenceAndAssertResponse(Ok, Idle)
      val expectedResult = StepListResult(None)
      assertCurrentSequence(expectedResult)
    }

    "abort the given sequence in InProgress state | ESW-155, ESW-137" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      abortSequenceAndAssertResponse(Ok, InProgress)
    }
  }

  "Idle -> Unhandled" in {
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._
    val cmds = List(command1, command2)

    assertUnhandled(
      Idle,
      StartSequence,
      AbortSequence,
      GoOnline,
      GoOnlineSuccess,
      GoOnlineFailed,
      MaybeNext,
      ReadyToExecuteNext,
      Update(Completed(Id()), _),
      GoIdle,
      GoneOffline,
      Add(cmds, _),
      Prepend(cmds, _),
      Replace(Id(), cmds, _),
      InsertAfter(Id(), cmds, _),
      Delete(Id(), _),
      AddBreakpoint(Id(), _),
      RemoveBreakpoint(Id(), _),
      Pause,
      Resume,
      Reset,
      ShutdownComplete
    )
  }

  "Loaded -> Unhandled" in {
    val sequencerSetup = SequencerTestSetup.loaded(sequence)
    import sequencerSetup._

    assertUnhandled(
      Loaded,
      LoadSequence(sequence, _),
      LoadAndStartSequenceInternal(sequence, _),
      Update(Completed(Id()), _),
      GoOnline,
      GoOnlineSuccess,
      GoOnlineFailed,
      MaybeNext,
      ReadyToExecuteNext,
      PullNext,
      MaybeNext,
      ReadyToExecuteNext,
      GoIdle,
      ShutdownComplete
    )
  }

  "InProgress -> Unhandled" in {
    val sequencerSetup = SequencerTestSetup.inProgress(sequence)
    import sequencerSetup._

    assertUnhandled(
      InProgress,
      LoadSequence(sequence, _),
      StartSequence,
      LoadAndStartSequenceInternal(sequence, _),
      GoOnline,
      GoOnlineSuccess,
      GoOnlineFailed,
      GoOffline,
      GoneOffline,
      ShutdownComplete
    )
  }
}
