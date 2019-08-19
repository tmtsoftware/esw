package esw.ocs.core

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.util.Timeout
import csw.command.client.messages.sequencer.LoadAndStartSequence
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.SequencerState.{Idle, InProgress, Loaded}
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages._
import esw.ocs.api.models.{Step, StepList}

class SequencerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val sequence = Sequence(Id(), Seq(command1, command2))

  implicit val timeoutDuration: Timeout = timeout

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

  "LoadSequence" must {
    "load the given sequence in idle state" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._
      assertSequencerIsLoaded(Ok)
    }

    "fail when given sequence contains duplicate Ids" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))

      val sequencerSetup = new SequencerTestSetup(invalidSequence)
      import sequencerSetup._

      assertSequencerIsLoaded(DuplicateIdsFound)
    }
  }

  "StartSequence" must {
    "start executing a sequence when sequencer is loaded" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._
      mockAllCommandResponses()
      assertSequencerIsLoaded(Ok)

      val seqResProbe = createTestProbe[SequenceResponse]
      sequencerActor ! StartSequence(seqResProbe.ref)
      pullAndAssertSequenceCompletion()
      seqResProbe.expectMessage(SequenceResult(Completed(sequence.runId)))
    }
  }

  "LoadAndStartSequence" must {
    "load and process sequence in idle state" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._

      mockAllCommandResponses()

      val probe = createTestProbe[SubmitResponse]
      sequencerActor ! LoadAndStartSequence(sequence, probe.ref)
      pullAndAssertSequenceCompletion()
      probe.expectMessage(Completed(sequence.runId))
    }

    "fail when given sequence contains duplicate Ids" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))

      val sequencerSetup = new SequencerTestSetup(invalidSequence)
      import sequencerSetup._

      val loadSeqResProbe = createTestProbe[SubmitResponse]
      sequencerActor ! LoadAndStartSequence(invalidSequence, loadSeqResProbe.ref)
      loadSeqResProbe.expectMessage(Error(invalidSequence.runId, DuplicateIdsFound.description))
    }
  }

  "GetSequence" must {
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._

    "return None when in Idle state" in {
      assertCurrentSequence(StepListResult(None))
    }

    "return sequence when in loaded state" in {
      assertSequencerIsLoaded(Ok)
      assertCurrentSequence(StepListResult(StepList(sequence).toOption))
    }
  }

  "GetPreviousSequence" must {
    "return None when sequencer has not started executing any sequence" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._
      // in idle state
      assertPreviousSequence(StepListResult(None))
      assertSequencerIsLoaded(Ok)
      // in loaded state
      assertPreviousSequence(StepListResult(None))
    }

    "return previous sequence after new sequence is loaded" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._

      mockAllCommandResponses()

      val loadAndStartResProbe = createTestProbe[SubmitResponse]
      sequencerActor ! LoadAndStartSequence(sequence, loadAndStartResProbe.ref)
      pullAndAssertSequenceCompletion()

      loadAndStartResProbe.expectMessage(Completed(sequence.runId))
      assertSequencerIsLoaded(Ok)

      val expectedPreviousSequence = StepListResult(
        Some(
          StepList(
            sequence.runId,
            List(
              Step(command1, Finished.Success(Completed(command1.runId)), hasBreakpoint = false),
              Step(command2, Finished.Success(Completed(command2.runId)), hasBreakpoint = false)
            )
          )
        )
      )

      assertPreviousSequence(expectedPreviousSequence)
    }
  }

  "Add" must {
    "add commands when sequence is loaded" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._

      assertSequencerIsLoaded(Ok)

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Add(List(command3), probe.ref)
      probe.expectMessage(Ok)

      val updatedSequence = sequence.copy(commands = Seq(command1, command2, command3))
      assertCurrentSequence(StepListResult(StepList(updatedSequence).toOption))
    }

    "add commands when sequence is in progress" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._

      assertSequencerIsInProgress()

      val probe = createTestProbe[OkOrUnhandledResponse]
      sequencerActor ! Add(List(command3), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        StepListResult(
          Some(StepList(sequence.runId, List(Step(command1, InFlight, hasBreakpoint = false), Step(command2), Step(command3))))
        )
      )
    }
  }

  "Pause" must {
    "pause sequencer when it is in-progress" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._

      assertSequencerIsInProgress()
      assertSequenceIsPaused()

      val expected = StepListResult(
        Some(
          StepList(
            sequence.runId,
            List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = true))
          )
        )
      )
      assertCurrentSequence(expected)
    }

    "pause sequencer when it is in loaded state" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._

      assertSequencerIsLoaded(Ok)
      assertSequenceIsPaused()

      val expected = StepListResult(
        Some(
          StepList(
            sequence.runId,
            List(Step(command1, Pending, hasBreakpoint = true), Step(command2, Pending, hasBreakpoint = false))
          )
        )
      )
      assertCurrentSequence(expected)
    }
  }

  "Resume" must {
    "resume a paused sequence when sequencer is in-progress" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._

      assertSequencerIsInProgress()
      assertSequenceIsPaused()

      val expectedPausedSequence = StepListResult(
        Some(
          StepList(
            sequence.runId,
            List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = true))
          )
        )
      )
      assertCurrentSequence(expectedPausedSequence)
      assertSequenceIsResumed()

      val expectedResumedSequence = StepListResult(
        Some(
          StepList(
            sequence.runId,
            List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
          )
        )
      )
      assertCurrentSequence(expectedResumedSequence)
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
      ShutdownComplete
    )
  }

  "Loaded -> Unhandled" in {
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._
    assertSequencerIsLoaded(Ok)

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
    val sequencerSetup = new SequencerTestSetup(sequence)
    import sequencerSetup._
    assertSequencerIsInProgress()

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

  "AbortSequence" must {
    "abort the given sequence in Loaded state | ESW-155, ESW-137" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._
      assertSequencerIsLoaded(Ok)
      assertSequenceIsAborted()
      val expectedResult = StepListResult(Some(StepList(sequence.runId, List.empty)))
      assertCurrentSequence(expectedResult)
    }

    "abort the given sequence in InProgress state | ESW-155, ESW-137" in {
      val sequencerSetup = new SequencerTestSetup(sequence)
      import sequencerSetup._
      assertSequencerIsInProgress()
      assertSequenceIsAborted()
    }
  }
}
