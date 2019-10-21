package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SubmitSequenceAndWait
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.logging.models.Level.DEBUG
import csw.logging.models.LogMetadata
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepStatus.{InFlight, Pending}
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol._
import esw.ocs.impl.messages.SequencerMessages.{AbortSequence, AddBreakpoint, QueryFinal, _}
import esw.ocs.impl.messages.SequencerState.{Idle, InProgress, Loaded, Offline}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Future, Promise}
import scala.util.Success

class SequencerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val command4 = Setup(Prefix("esw.test"), CommandName("command-4"), None)
  private val sequence = Sequence(Id(), Seq(command1, command2))

  private implicit val timeoutDuration: Timeout = timeout
  private val maxWaitForExpectNoMessage         = 200.millis

  def Finished(id: Id): StepStatus = StepStatus.Finished.Success(Completed(id))

  "LoadSequence" must {
    "load the given sequence in idle state | ESW-145" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadSequenceAndAssertResponse(Ok)
    }

    "load the given sequence in loaded state | ESW-145" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._
      loadSequenceAndAssertResponse(Ok)
    }

    "fail when given sequence contains duplicate Ids | ESW-145" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))
      val sequencerSetup  = SequencerTestSetup.idle(invalidSequence)
      import sequencerSetup._

      loadSequenceAndAssertResponse(DuplicateIdsFound)
    }
  }

  "StartSequence" must {
    "start executing a sequence when sequencer is loaded | ESW-145, ESW-154" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! StartSequence(probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(Ok)
    }
  }

  "LoadAndStartSequence" must {
    "load and start executing a sequence | ESW-145, ESW-154, ESW-221" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val probe = createTestProbe[LoadSequenceResponse]
      sequencerActor ! SubmitSequence(sequence, probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(Ok)
    }

    "fail when given sequence contains duplicate Ids | ESW-145, ESW-154, ESW-221" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))
      val sequencerSetup  = SequencerTestSetup.idle(invalidSequence)
      import sequencerSetup._

      val probe = createTestProbe[LoadSequenceResponse]
      sequencerActor ! SubmitSequence(invalidSequence, probe.ref)
      probe.expectMessage(DuplicateIdsFound)
    }

    "return Ok even if the processing of sequence fails | ESW-145, ESW-154, ESW-221" in {
      val sequence1      = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.idle(sequence1)
      import sequencerSetup._

      val probe = createTestProbe[LoadSequenceResponse]
      sequencerActor ! SubmitSequence(sequence1, probe.ref)
      probe.expectMessage(Ok)

      val sequenceError = Error(command1.runId, "Some error")
      assertSequencerState(InProgress)
      mockCommand(command1.runId, Future.successful(sequenceError))
      pullNextCommand()

      val expectedSteps = List(Step(command1, status = StepStatus.Finished.Failure(sequenceError), hasBreakpoint = false))
      assertCurrentSequence(Some(StepList(sequence1.runId, expectedSteps)))
    }
  }

  "LoadAndProcessSequence" must {
    "load and process sequence in idle state | ESW-145, ESW-154" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val probe = createTestProbe[SubmitResponse]
      sequencerActor ! SubmitSequenceAndWait(sequence, probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(Completed(sequence.runId))
    }

    "fail when given sequence contains duplicate Ids | ESW-145, ESW-154" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))

      val sequencerSetup = SequencerTestSetup.idle(invalidSequence)
      import sequencerSetup._

      val probe = createTestProbe[SubmitResponse]
      sequencerActor ! SubmitSequenceAndWait(invalidSequence, probe.ref)
      probe.expectMessage(Error(invalidSequence.runId, DuplicateIdsFound.msg))
    }
  }

  "QuerySequenceResponse" must {
    "return nothing when sequencer is Idle and hasn't executed any sequence | ESW-221" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val seqResProbe = createTestProbe[SequenceResponse]
      sequencerActor ! QueryFinal(seqResProbe.ref)
      seqResProbe.expectNoMessage(maxWaitForExpectNoMessage)
    }

    "return Sequence result with Completed when sequencer is in loaded state | ESW-145, ESW-154, ESW-221" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val seqResProbe = createTestProbe[SequenceResponse]
      sequencerActor ! QueryFinal(seqResProbe.ref)
      seqResProbe.expectNoMessage(maxWaitForExpectNoMessage)

      val startSeqProbe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! StartSequence(startSeqProbe.ref)
      startSeqProbe.expectMessage(Ok)
      pullAllStepsAndAssertSequenceIsFinished()

      seqResProbe.expectMessage(SequenceResult(Completed(sequence.runId)))
    }

    "return Sequence result with Completed when sequencer is inProgress state | ESW-145, ESW-154, ESW-221" in {
      val sequence1      = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.loaded(sequence1)
      import sequencerSetup._
      val promise = Promise[SubmitResponse]
      mockCommand(command1.runId, promise.future)

      val startSeqProbe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! StartSequence(startSeqProbe.ref)
      startSeqProbe.expectMessage(Ok)

      pullNextCommand()
      assertSequencerState(InProgress)

      val seqResProbe = createTestProbe[SequenceResponse]
      sequencerActor ! QueryFinal(seqResProbe.ref)
      seqResProbe.expectNoMessage(maxWaitForExpectNoMessage)

      promise.complete(Success(Completed(command1.runId)))
      seqResProbe.expectNoMessage(maxWaitForExpectNoMessage)

      assertSequenceIsFinished()
      seqResProbe.expectMessage(SequenceResult(Completed(sequence1.runId)))
    }

    "return Sequence result with Completed when sequencer has finished executing a sequence | ESW-145, ESW-154, ESW-221" in {
      val sequencerSetup = SequencerTestSetup.finished(sequence)
      import sequencerSetup._

      val seqResProbe = createTestProbe[SequenceResponse]
      sequencerActor ! QueryFinal(seqResProbe.ref)

      seqResProbe.expectMessage(SequenceResult(Completed(sequence.runId)))
    }

  }

  "GetSequence" must {
    val sequencerSetup = SequencerTestSetup.idle(sequence)
    import sequencerSetup._

    "return None when in Idle state | ESW-157" in {
      assertCurrentSequence(None)
    }

    "return sequence when in Loaded state | ESW-157" in {
      loadSequenceAndAssertResponse(Ok)
      assertCurrentSequence(StepList(sequence).toOption)
    }

    "return sequence when in inProgress state | ESW-157" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      loadAndStartSequenceThenAssertInProgress()
      mockCommand(command1.runId, Promise().future) //  future will not complete
      pullNextCommand()                             // why is this necessary?

      val expectedSteps = List(
        Step(command1, InFlight, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )

      assertCurrentSequence(Some(StepList(sequence.runId, expectedSteps)))
    }

    "return sequence when in finished state | ESW-157" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      loadAndStartSequenceThenAssertInProgress()

      pullAllStepsAndAssertSequenceIsFinished()

      val expectedSteps = List(
        Step(command1, Finished(command1.runId), hasBreakpoint = false),
        Step(command2, Finished(command2.runId), hasBreakpoint = false)
      )

      assertCurrentSequence(Some(StepList(sequence.runId, expectedSteps)))
    }
  }

  "GetSequencerState" must {
    "return current state of the sequencer" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      assertSequencerState(Idle)

      loadAndStartSequenceThenAssertInProgress()
      assertSequencerState(InProgress)

      pullAllStepsAndAssertSequenceIsFinished()
      assertSequencerState(Idle)

      goOfflineAndAssertResponse(Ok, Future.successful(Done))
      assertSequencerState(Offline)

      goOnlineAndAssertResponse(Ok, Future.successful(Done))
      assertSequencerState(Idle)
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
      assertCurrentSequence(StepList(updatedSequence).toOption)
    }

    "add commands when sequence is in progress | ESW-114" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Add(List(command3), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        Some(
          StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2), Step(command3)))
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
      assertCurrentSequence(StepList(updatedSequence).toOption)
    }

    "add steps before first pending step in InProgress state | ESW-113" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Prepend(List(command3), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        Some(
          StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command3), Step(command2)))
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

      assertCurrentSequence(beforePauseStepList)

      pauseAndAssertResponse(Ok)

      val afterPauseStepList = Some(
        StepList(
          sequence.runId,
          List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = true))
        )
      )

      assertCurrentSequence(afterPauseStepList)
    }

    "pause sequencer when it is in Loaded state | ESW-104" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val beforePauseStepList = StepList(
        sequence.runId,
        List(Step(command1, Pending, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
      )
      assertCurrentSequence(Some(beforePauseStepList))

      pauseAndAssertResponse(Ok)

      val afterPauseStepList = StepList(
        sequence.runId,
        List(Step(command1, Pending, hasBreakpoint = true), Step(command2, Pending, hasBreakpoint = false))
      )

      assertCurrentSequence(Some(afterPauseStepList))
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

      val expectedPausedSequence = Some(StepList(sequence.runId, expectedPausedSteps))

      val expectedResumedSteps = List(
        Step(command1, Pending, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )

      val expectedResumedSequence = Some(StepList(sequence.runId, expectedResumedSteps))

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
      val expectedPausedSequence = Some(StepList(sequence.runId, expectedPausedSteps))

      val expectedResumedSteps = List(
        Step(command1, InFlight, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )
      val expectedResumedSequence = Some(StepList(sequence.runId, expectedResumedSteps))

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

      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

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
      eventually(getSequence().get.steps(1).isFinished should ===(true))

      replaceAndAssertResponse(command1.runId, List(command3, command4), CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail if inflight step is tried to be replaced in InProgress state | ESW-108" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      eventually(getSequence().get.steps.head.isInFlight should ===(true))

      replaceAndAssertResponse(command1.runId, List(command3, command4), CannotOperateOnAnInFlightOrFinishedStep)
    }

    "replace pending step in InProgress state | ESW-108" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps = List(
        Step(command1, InFlight, hasBreakpoint = false),
        Step(command3, Pending, hasBreakpoint = false),
        Step(command4, Pending, hasBreakpoint = false)
      )

      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

      eventually(getSequence().get.steps.head.isInFlight should ===(true))

      replaceAndAssertResponse(command2.runId, List(command3, command4), Ok)
      assertCurrentSequence(expectedSequence)
    }

  }

  "Delete" must {
    "delete steps when sequencer is Loaded | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, Pending, hasBreakpoint = false))
      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(command2.runId, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }

    "delete steps when sequencer is InProgress | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, InFlight, hasBreakpoint = false))
      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(command2.runId, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }

    "cannot delete inFlight steps when sequencer is InProgress | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(command1.runId, deleteResProbe.ref)
      deleteResProbe.expectMessage(CannotOperateOnAnInFlightOrFinishedStep)
      assertCurrentSequence(expectedSequence)
    }
  }

  "InsertAfter" must {
    "insert steps after provided id when sequencer is in Loaded state | ESW-111" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val cmdsToInsert = List(command3, command4)

      val expectedSequenceAfterInsertion =
        Some(
          StepList(
            sequence.runId,
            List(Step(command1), Step(command3), Step(command4), Step(command2))
          )
        )

      val insertResProbe = TestProbe[GenericResponse]()
      sequencerActor ! InsertAfter(command1.runId, cmdsToInsert, insertResProbe.ref)
      insertResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequenceAfterInsertion)
    }

    "insert steps after provided id when sequencer is in InProgress state | ESW-111" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val cmdsToInsert = List(command3, command4)

      val expectedSequenceAfterInsertion =
        Some(
          StepList(
            sequence.runId,
            List(Step(command1, InFlight, hasBreakpoint = false), Step(command3), Step(command4), Step(command2))
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
      stepListResult.get.steps.forall(_.isInFlight) should ===(true)

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
        Some(StepList(sequence.runId, List(Step(command1), Step(command2).copy(hasBreakpoint = true))))

      addBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakPoint =
        Some(StepList(sequence.runId, List(Step(command1), Step(command2).copy(hasBreakpoint = false))))

      removeBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterRemovingBreakPoint)
    }

    "add and delete breakpoint to/from provided id when step status is Pending and sequencer is in InProgress state | ESW-106, ESW-107" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSequenceAfterAddingBreakpoint =
        Some(StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = true))))

      addBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakpoint =
        Some(StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = false))))

      removeBreakpointAndAssertResponse(command2.runId, Ok)
      assertCurrentSequence(expectedSequenceAfterRemovingBreakpoint)
    }
  }

  "Reset" must {
    "discard complete sequence in Loaded state" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      assertCurrentSequence(Some(StepList(sequence).rightValue))
      resetAndAssertResponse(Ok)
      assertSequencerState(Idle)
      assertCurrentSequence(None)
    }

    "discard pending steps in InProgress state" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      resetAndAssertResponse(Ok)
      assertSequencerState(InProgress)
      assertCurrentSequence(Some(StepList(sequence.runId, List(Step(command1, status = InFlight, hasBreakpoint = false)))))
    }
  }

  "AbortSequence" must {
    "abort the given sequence in InProgress state | ESW-155, ESW-137" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      abortSequenceAndAssertResponse(Ok, InProgress)
    }
  }

  "GoOnline" must {
    "go to Idle state when sequencer is Offline | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      goOnlineAndAssertResponse(Ok, Future.successful(Done))
      assertSequencerState(Idle)
      // try loading a sequence to ensure sequencer is online
      loadSequenceAndAssertResponse(Ok)
    }

    "remain in offline state if online handlers fail | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      goOnlineAndAssertResponse(GoOnlineHookFailed, Future.failed(new RuntimeException("GoOnline Hook Failed")))
      assertSequencerState(Offline)
    }
  }

  "GoOffline" must {
    "go to Offline state from Idle state | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      goOfflineAndAssertResponse(Ok, Future.successful(Done))
      assertSequencerState(Offline)
    }

    "clear history of the last executed sequence | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.finished(sequence)
      import sequencerSetup._

      goOfflineAndAssertResponse(Ok, Future.successful(Done))

      assertCurrentSequence(None)
    }

    "not go to Offline state even if the offline handlers fail | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      goOfflineAndAssertResponse(GoOfflineHookFailed, Future.failed(new RuntimeException("GoOffline Hook Failed")))
      assertSequencerState(Idle)
    }
  }

  "ReadyToExecuteNext" must {
    "return Ok immediately when a new step is available for execution" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadAndStartSequenceThenAssertInProgress()

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectMessage(Ok)
    }

    "wait till completion of current command" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadAndStartSequenceThenAssertInProgress()

      // long running command
      val promise = Promise[SubmitResponse]
      mockCommand(command1.runId, promise.future)
      pullNextCommand()

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      // finish first command
      promise.complete(Success(Completed(command1.runId)))

      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectMessage(Ok)
    }

    "wait till a sequence is started" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      // start the sequence and assert Ok is sent to the readyToExecuteNext subscriber as soon as a step is ready
      sequencerActor ! StartSequence(createTestProbe[OkOrUnhandledResponse].ref)
      probe.expectMessage(Ok)
    }

    "wait till next sequence is received if current sequence is finished" in {
      val sequencerSetup = SequencerTestSetup.finished(sequence)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      loadAndStartSequenceThenAssertInProgress()
      probe.expectMessage(Ok)
    }

    "wait till next sequence is received if sequencer went offline" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      goOnlineAndAssertResponse(Ok, Future.successful(Done))
      loadAndStartSequenceThenAssertInProgress()
      probe.expectMessage(Ok)
    }

    "wait till sequence is resumed in case of a paused sequence" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadAndStartSequenceThenAssertInProgress()
      mockCommand(command1.runId, Future.successful(Completed(command1.runId)))
      pullNextCommand()

      pauseAndAssertResponse(Ok)

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      resumeAndAssertResponse(Ok)
      probe.expectMessage(Ok)
    }
  }

  "MayBeNext" must {
    "return next pending command" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      mayBeNextAndAssertResponse(Some(Step(command2)))
    }

    "return None if sequencer is paused" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._
      pauseAndAssertResponse(Ok)

      mayBeNextAndAssertResponse(None)
    }

    "return None if sequencer is finished" in {
      val sequencerSetup = SequencerTestSetup.finished(sequence)
      import sequencerSetup._

      mayBeNextAndAssertResponse(None)
    }

    "return None if there's no pending step to be executed" in {
      val sequencerSetup = SequencerTestSetup.inProgress(Sequence(command1))
      import sequencerSetup._

      mayBeNextAndAssertResponse(None)
    }
  }

  "Update" must {
    "update the given step with successful response" in {
      val sequence       = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = TestProbe[OkOrUnhandledResponse]
      sequencerActor ! Update(Completed(command1.runId), probe.ref)

      assertCurrentSequence(Some(StepList(sequence.runId, List(Step(command1, Finished(command1.runId), hasBreakpoint = false)))))
    }

    "update the given step with error response" in {
      val sequence       = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val error = Error(command1.runId, "some")
      val probe = TestProbe[OkOrUnhandledResponse]
      sequencerActor ! Update(error, probe.ref)

      assertCurrentSequence(
        Some(StepList(sequence.runId, List(Step(command1, StepStatus.Finished.Failure(error), hasBreakpoint = false))))
      )
    }
  }

  "DiagnosticMode" must {
    "execute the diagnostic handler and return the diagnostic response | ESW-118" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val startTime = UTCTime.now()
      val hint      = "engineering"

      diagnosticModeAndAssertResponse(startTime, hint, Ok, Future.successful(Done))
    }
  }

  "OperationsMode" must {
    "execute the operations handler and return the operations response | ESW-118" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      operationsModeAndAssertResponse(Ok, Future.successful(Done))
    }
  }

  "LogControlMessages" must {
    "set and get log level for component name | ESW-183" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._
      val logMetadataProbe = TestProbe[LogMetadata]

      sequencerActor ! SetComponentLogLevel(sequencerName, DEBUG)
      sequencerActor ! GetComponentLogMetadata(sequencerName, logMetadataProbe.ref)

      val logMetadata = logMetadataProbe.expectMessageType[LogMetadata]

      logMetadata.componentLevel shouldBe DEBUG
    }
  }

  "Idle -> Unhandled | ESW-104, ESW-105, ESW-106, ESW-107, ESW-108, ESW-110, ESW-111, ESW-112, ESW-113, ESW-114, ESW-154, ESW-155, ESW-194" in {
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._
    val cmds = List(command1, command2)

    assertUnhandled(
      Idle,
      StartSequence, //ESW-154
      AbortSequence, //ESW-155
      AbortSequenceComplete,
      GoOnline,
      GoOnlineSuccess,
      GoOnlineFailed,
      Update(Completed(Id()), _),
      GoIdle,
      GoOfflineSuccess,
      GoOfflineFailed,
      Add(cmds, _),               //ESW-114
      Prepend(cmds, _),           // ESW-113 : should not allow to prepend in a finished Sequence
      Replace(Id(), cmds, _),     //ESW-108
      InsertAfter(Id(), cmds, _), // ESW-111 : Error should be thrown when inserting in a finished sequence
      Delete(Id(), _),            //ESW-112
      AddBreakpoint(Id(), _),     //ESW-106
      RemoveBreakpoint(Id(), _),  //ESW-107
      Pause,                      //ESW-104
      Resume,                     //ESW-105
      Reset                       //ESW-110
    )
  }

  "Loaded -> Unhandled | ESW-145, ESW-155, ESW-194" in {
    val sequencerSetup = SequencerTestSetup.loaded(sequence)
    import sequencerSetup._

    assertUnhandled(
      Loaded,
      AbortSequence, //ESW-155
      AbortSequenceComplete,
      SubmitSequenceAndWaitInternal(sequence, _),
      Update(Completed(Id()), _),
      GoOnline,
      GoOnlineSuccess,
      GoOnlineFailed,
      PullNext,
      GoIdle
    )
  }

  "InProgress -> Unhandled | ESW-145, ESW-154, ESW-194" in {
    val sequencerSetup = SequencerTestSetup.inProgress(sequence)
    import sequencerSetup._

    assertUnhandled(
      InProgress,
      LoadSequence(sequence, _),
      StartSequence, //ESW-154
      SubmitSequenceAndWaitInternal(sequence, _),
      GoOnline,
      GoOnlineSuccess,
      GoOnlineFailed,
      GoOffline,
      GoOfflineSuccess,
      GoOfflineFailed
    )
  }

  "Offline -> Unhandled | ESW-194, ESW-104, ESW-105, ESW-106, ESW-107, ESW-108, ESW-110, ESW-111, ESW-112, ESW-113, ESW-114, ESW-154, ESW-155" in {
    val sequencerSetup = SequencerTestSetup.offline(sequence)
    import sequencerSetup._
    val cmds = List(command1, command2)

    assertUnhandled(
      Offline,
      //Should not accept these commands in offline state
      SubmitSequenceAndWaitInternal(sequence, _),
      LoadSequence(sequence, _),
      StartSequence, //ESW-154
      AbortSequence, //ESW-155
      AbortSequenceComplete,
      Add(cmds, _),               //ESW-114
      Prepend(cmds, _),           //ESW-113
      Replace(Id(), cmds, _),     //ESW-108
      InsertAfter(Id(), cmds, _), //ESW-111
      Delete(Id(), _),            //ESW-112
      AddBreakpoint(Id(), _),     //ESW-106
      RemoveBreakpoint(Id(), _),  //ESW-107
      Pause,                      //ESW-104
      Resume,                     //ESW-105
      Reset,                      //ESW-110
      GoOnlineSuccess,
      GoOnlineFailed,
      GoOfflineSuccess,
      GoOfflineFailed,
      GoIdle,
      PullNext,
      Update(Completed(Id()), _),
      QueryFinal
    )
  }
}
