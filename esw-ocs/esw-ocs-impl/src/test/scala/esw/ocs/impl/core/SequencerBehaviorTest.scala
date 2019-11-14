package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import csw.command.client.messages.sequencer.SequencerMsg.{QueryFinal, SubmitSequenceAndWait}
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.logging.client.commons.LogAdminUtil
import csw.logging.models.Level.{DEBUG, INFO}
import csw.logging.models.LogMetadata
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol._
import esw.ocs.impl.messages.SequencerMessages.{AbortSequence, AddBreakpoint, QueryFinalInternal, _}
import esw.ocs.impl.messages.SequencerState.{Idle, InProgress, Loaded, Offline}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val command4 = Setup(Prefix("esw.test"), CommandName("command-4"), None)
  private val sequence = Sequence(Id(), Seq(command1, command2))

  private val maxWaitForExpectNoMessage = 200.millis

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
  }

  "StartSequence" must {
    "start executing a sequence when sequencer is loaded | ESW-145, ESW-154" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = createTestProbe[SequenceResponse]
      sequencerActor ! StartSequence(probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(SequenceResult(Started(sequence.runId)))
    }
  }

  "SubmitSequence" must {
    "load and start executing a sequence | ESW-145, ESW-154, ESW-221" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val probe = createTestProbe[SequenceResponse]
      sequencerActor ! SubmitSequence(sequence, probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(SequenceResult(Started(sequence.runId)))
    }

    "return Ok even if the processing of sequence fails | ESW-145, ESW-154, ESW-221" in {
      val sequence1      = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.idle(sequence1)
      import sequencerSetup._

      val client = createTestProbe[SequenceResponse]
      sequencerActor ! SubmitSequence(sequence1, client.ref)
      client.expectMessage(SequenceResult(Started(sequence1.runId)))
      assertSequencerState(InProgress)

      startPullNext()
      val message = "Some error"
      finishStepWithError(message)
      assertSequencerState(Idle)

      val qfProbe = createTestProbe[SubmitResponse]()
      sequencerActor ! QueryFinal(qfProbe.ref)
      qfProbe.expectMessage(Error(sequence1.runId, message))
    }
  }

  "SubmitSequenceAndWait" must {
    "load and process sequence in idle state | ESW-145, ESW-154" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val probe = createTestProbe[SubmitResponse]
      sequencerActor ! SubmitSequenceAndWait(sequence, probe.ref)
      pullAllStepsAndAssertSequenceIsFinished()
      probe.expectMessage(Completed(sequence.runId))
    }
  }

  "QuerySequenceResponse" must {
    "return error response when sequencer is Idle and hasn't executed any sequence | ESW-221" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val seqResProbe = createTestProbe[SubmitResponse]
      sequencerActor ! QueryFinal(seqResProbe.ref)
      seqResProbe.expectMessageType[Error]
    }

    "return Sequence result with Completed when sequencer is in loaded state | ESW-145, ESW-154, ESW-221" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val seqResProbe = createTestProbe[SequenceResponse]
      sequencerActor ! QueryFinalInternal(seqResProbe.ref)
      seqResProbe.expectNoMessage(maxWaitForExpectNoMessage)

      val startSeqProbe = createTestProbe[SequenceResponse]
      sequencerActor ! StartSequence(startSeqProbe.ref)
      startSeqProbe.expectMessage(SequenceResult(Started(sequence.runId)))
      pullAllStepsAndAssertSequenceIsFinished()

      seqResProbe.expectMessage(SequenceResult(Completed(sequence.runId)))
    }

    "return Sequence result with Completed when sequencer is inProgress state | ESW-145, ESW-154, ESW-221" in {
      val sequence1      = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.loaded(sequence1)
      import sequencerSetup._

      val startSeqProbe = createTestProbe[SequenceResponse]
      sequencerActor ! StartSequence(startSeqProbe.ref)
      startSeqProbe.expectMessage(SequenceResult(Started(sequence1.runId)))

      startPullNext()
      assertSequencerState(InProgress)

      val seqResProbe = createTestProbe[SubmitResponse]
      sequencerActor ! QueryFinal(seqResProbe.ref)
      seqResProbe.expectNoMessage(maxWaitForExpectNoMessage)

      finishStepWithSuccess()
      assertSequenceIsFinished()

      seqResProbe.expectMessage(Completed(sequence1.runId))
    }

    "return Sequence result with Completed when sequencer has finished executing a sequence | ESW-145, ESW-154, ESW-221" in {
      val sequencerSetup = SequencerTestSetup.finished(sequence)
      import sequencerSetup._

      val seqResProbe = createTestProbe[SubmitResponse]
      sequencerActor ! QueryFinal(seqResProbe.ref)

      seqResProbe.expectMessage(Completed(sequence.runId))
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
      assertCurrentSequence(StepList(sequence))
    }

    "return sequence when in inProgress state | ESW-157" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      loadAndStartSequenceThenAssertInProgress()
      startPullNext()

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
        Step(command1, Finished.Success, hasBreakpoint = false),
        Step(command2, Finished.Success, hasBreakpoint = false)
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
      assertCurrentSequence(StepList(updatedSequence))
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
      assertCurrentSequence(StepList(updatedSequence))
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
      val sequencerSetup = SequencerTestSetup.inProgressWithFirstCommandComplete(sequence)
      import sequencerSetup._

      val beforePauseStepList = Some(
        StepList(
          sequence.runId,
          List(Step(command1, Finished.Success, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
        )
      )

      assertCurrentSequence(beforePauseStepList)

      //Engine can execute next step as 1st step is completed
      assertEngineCanExecuteNext(isReadyToExecuteNext = true)

      pauseAndAssertResponse(Ok)

      val afterPauseStepList = Some(
        StepList(
          sequence.runId,
          List(Step(command1, Finished.Success, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = true))
        )
      )

      //Engine can NOT execute next step as 1st step is completed but 2nd one is paused
      assertEngineCanExecuteNext(isReadyToExecuteNext = false)
      assertCurrentSequence(afterPauseStepList)
    }
  }

  "Resume" must {
    "resume a paused sequence when sequencer is InProgress | ESW-105" in {
      val sequencerSetup = SequencerTestSetup.inProgressWithFirstCommandComplete(sequence)
      import sequencerSetup._

      val expectedPausedSteps = List(
        Step(command1, Finished.Success, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = true)
      )
      val expectedPausedSequence = Some(StepList(sequence.runId, expectedPausedSteps))

      val expectedResumedSteps = List(
        Step(command1, Finished.Success, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )
      val expectedResumedSequence = Some(StepList(sequence.runId, expectedResumedSteps))

      pauseAndAssertResponse(Ok)
      //Sequence is paused so engine can NOT execute next step
      assertEngineCanExecuteNext(isReadyToExecuteNext = false)
      assertCurrentSequence(expectedPausedSequence)
      resumeAndAssertResponse(Ok)
      //Sequence is resumed so engine can execute next step
      assertEngineCanExecuteNext(isReadyToExecuteNext = true)
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

      val stepId1 = getSequence().get.steps(0).id

      replaceAndAssertResponse(stepId1, List(command3, command4), Ok)
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

      finishStepWithSuccess()
      startPullNext()
      val step1 = getSequence().get.steps(0)
      val step2 = getSequence().get.steps(1)
      eventually(step1.isFinished should ===(true))

      replaceAndAssertResponse(step2.id, List(command3, command4), CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail if inflight step is tried to be replaced in InProgress state | ESW-108" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val step1 = getSequence().get.steps.head

      eventually(step1.isInFlight should ===(true))

      replaceAndAssertResponse(step1.id, List(command3, command4), CannotOperateOnAnInFlightOrFinishedStep)
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

      val step1 = getSequence().get.steps.head
      val step2 = getSequence().get.steps(1)

      eventually(step1.isInFlight should ===(true))

      replaceAndAssertResponse(step2.id, List(command3, command4), Ok)
      assertCurrentSequence(expectedSequence)
    }

  }

  "Delete" must {
    "delete steps when sequencer is Loaded | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, Pending, hasBreakpoint = false))
      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

      val step2 = getSequence().get.steps(1)

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(step2.id, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }

    "delete steps when sequencer is InProgress | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, InFlight, hasBreakpoint = false))
      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

      val step2 = getSequence().get.steps(1)

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(step2.id, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }

    "cannot delete inFlight steps when sequencer is InProgress | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
      val expectedSequence = Some(StepList(sequence.runId, expectedSteps))

      val step1 = getSequence().get.steps.head

      val deleteResProbe = createTestProbe[GenericResponse]()
      sequencerActor ! Delete(step1.id, deleteResProbe.ref)
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

      val step1 = getSequence().get.steps.head

      val insertResProbe = TestProbe[GenericResponse]()
      sequencerActor ! InsertAfter(step1.id, cmdsToInsert, insertResProbe.ref)
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

      val step1 = getSequence().get.steps.head

      val insertResProbe = TestProbe[GenericResponse]()
      sequencerActor ! InsertAfter(step1.id, cmdsToInsert, insertResProbe.ref)
      insertResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequenceAfterInsertion)
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep when trying to insert before a InFlight step in InProgress state | ESW-111" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      startPullNext()
      val stepListResult = getSequence()
      stepListResult.get.steps.forall(_.isInFlight) should ===(true)

      val step1 = stepListResult.get.steps(0)

      val cmdsToInsert   = List(command3, command4)
      val insertResProbe = TestProbe[GenericResponse]()
      sequencerActor ! InsertAfter(step1.id, cmdsToInsert, insertResProbe.ref)
      insertResProbe.expectMessage(CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "AddBreakpoint and RemoveBreakpoint" must {
    "add and delete breakpoint to/from provided id when step status is Pending and sequencer is in Loaded state | ESW-106, ESW-107" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val expectedSequenceAfterAddingBreakpoint =
        Some(StepList(sequence.runId, List(Step(command1), Step(command2).copy(hasBreakpoint = true))))

      val step2 = getSequence().get.steps(1)

      addBreakpointAndAssertResponse(step2.id, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakPoint =
        Some(StepList(sequence.runId, List(Step(command1), Step(command2).copy(hasBreakpoint = false))))

      removeBreakpointAndAssertResponse(step2.id, Ok)
      assertCurrentSequence(expectedSequenceAfterRemovingBreakPoint)
    }

    "add and delete breakpoint to/from provided id when step status is Pending and sequencer is in InProgress state | ESW-106, ESW-107" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSequenceAfterAddingBreakpoint =
        Some(StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = true))))

      val step2 = getSequence().get.steps(1)

      addBreakpointAndAssertResponse(step2.id, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakpoint =
        Some(StepList(sequence.runId, List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = false))))

      removeBreakpointAndAssertResponse(step2.id, Ok)
      assertCurrentSequence(expectedSequenceAfterRemovingBreakpoint)
    }
  }

  "Reset" must {
    "discard complete sequence in Loaded state" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      assertCurrentSequence(Some(StepList(sequence)))
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

  "Stop" must {
    "stop the given sequence in InProgress state | ESW-156, ESW-138" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      stopAndAssertResponse(Ok, InProgress)
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

      startPullNext()

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)

      probe.expectNoMessage(maxWaitForExpectNoMessage)

      finishStepWithSuccess()

      probe.expectMessage(Ok)
    }

    "wait till a sequence is started" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      // start the sequence and assert Ok is sent to the readyToExecuteNext subscriber as soon as a step is ready
      sequencerActor ! StartSequence(createTestProbe[SequenceResponse].ref)
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
      startPullNext()

      pauseAndAssertResponse(Ok)

      val probe = TestProbe[Ok.type]
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)
      finishStepWithSuccess()
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
      sequencerActor ! StepSuccess(probe.ref)

      assertCurrentSequence(
        Some(StepList(sequence.runId, List(Step(command1, Finished.Success, hasBreakpoint = false))))
      )
    }

    "update the given step with error response" in {
      val sequence       = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val message = "some"
      val probe   = TestProbe[OkOrUnhandledResponse]
      sequencerActor ! StepFailure(message, probe.ref)

      assertCurrentSequence(
        Some(StepList(sequence.runId, List(Step(command1, Finished.Failure(message), hasBreakpoint = false))))
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

      sequencerActor ! GetComponentLogMetadata(sequencerName, logMetadataProbe.ref)

      val logMetadata1 = logMetadataProbe.expectMessageType[LogMetadata]

      logMetadata1.componentLevel shouldBe INFO
      val initialMetadata = LogAdminUtil.getLogMetadata(sequencerName)
      initialMetadata.componentLevel shouldBe INFO

      sequencerActor ! SetComponentLogLevel(sequencerName, DEBUG)
      sequencerActor ! GetComponentLogMetadata(sequencerName, logMetadataProbe.ref)

      val logMetadata2 = logMetadataProbe.expectMessageType[LogMetadata]
      logMetadata2.componentLevel shouldBe DEBUG

      // this verifies that log metadata is updated in LogAdminUtil
      val finalMetadata = LogAdminUtil.getLogMetadata(sequencerName)
      finalMetadata.componentLevel shouldBe DEBUG
    }
  }

  "Idle -> Unhandled | ESW-104, ESW-105, ESW-106, ESW-107, ESW-108, ESW-110, ESW-111, ESW-112, ESW-113, ESW-114, ESW-154, ESW-155, ESW-194, ESW-156" in {
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._
    val cmds = List(command1, command2)

    assertUnhandled(
      Idle,
      StartSequence, //ESW-154
      AbortSequence, //ESW-155
      AbortSequenceComplete,
      Stop, //ESW-156
      StopComplete,
      GoOnline,
      GoOnlineSuccess,
      GoOnlineFailed,
      StepSuccess,
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

  "Loaded -> Unhandled | ESW-145, ESW-155, ESW-194, ESW-156" in {
    val sequencerSetup = SequencerTestSetup.loaded(sequence)
    import sequencerSetup._

    assertUnhandled(
      Loaded,
      AbortSequence, //ESW-155
      AbortSequenceComplete,
      Stop, //ESW-156
      StopComplete,
      SubmitSequenceAndWaitInternal(sequence, _),
      StepSuccess,
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

  "Offline -> Unhandled | ESW-194, ESW-104, ESW-105, ESW-106, ESW-107, ESW-108, ESW-110, ESW-111, ESW-112, ESW-113, ESW-114, ESW-154, ESW-155, ESW-156" in {
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
      Stop, //ESW-156
      StopComplete,
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
      StepSuccess,
      QueryFinalInternal
    )
  }
}
