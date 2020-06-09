package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.location.api.models.AkkaLocation
import csw.logging.client.commons.LogAdminUtil
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.logging.models.Level.{DEBUG, INFO}
import csw.logging.models.LogMetadata
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.time.core.models.UTCTime
import esw.commons.BaseTestSuite
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState.{Idle, InProgress, Loaded, Offline}
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerBehaviorTest extends BaseTestSuite {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "sequencer-test-system")

  private val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val command3 = Setup(Prefix("esw.test"), CommandName("command-3"), None)
  private val command4 = Setup(Prefix("esw.test"), CommandName("command-4"), None)
  private val sequence = Sequence(command1, command2)

  private val maxWaitForExpectNoMessage = 200.millis

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  "LoadSequence" must {
    "load the given sequence in idle state | ESW-145, ESW-141" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadSequenceAndAssertResponse(Ok)
      assertSequencerState(Loaded) // ESW-141: state transitioned Idle -> Loaded
    }

    "load the given sequence in loaded state | ESW-145, ESW-141" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      loadSequenceAndAssertResponse(Ok)
      assertSequencerState(Loaded) // ESW-141: state stays Loaded
    }
  }

  "StartSequence" must {
    "start executing a sequence when sequencer is loaded after successful completion of new sequence handler| ESW-145, ESW-154, ESW-303" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.successful(Done) }

      val probe = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! StartSequence(probe.ref)
      assertSequencerState(InProgress)
      pullAllStepsAndAssertSequenceIsFinished()
      val sequenceResult = probe.expectMessageType[SubmitResult]
      sequenceResult.submitResponse shouldBe a[Started]

      verify(script).executeNewSequenceHandler() // ESW-303: varifies newSequenceHandler is called
    }

    "not start executing a sequence when sequencer is loaded and new sequence handler failed| ESW-303" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.failed(new RuntimeException) }

      val probe = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! StartSequence(probe.ref)
      assertSequenceNotStarted()
      val res = probe.expectMessageType[SequencerSubmitResponse]
      res should ===(NewSequenceHookFailed())

      verify(script).executeNewSequenceHandler() // ESW-303: varifies newSequenceHandler is called
    }
  }

  "SubmitSequence" must {
    "load and start executing a sequence after successful completion of new sequence handler| ESW-145, ESW-154, ESW-221, ESW-303" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.successful(Done) }

      val probe = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! SubmitSequenceInternal(sequence, probe.ref)

      pullAllStepsAndAssertSequenceIsFinished()
      val sequenceResult = probe.expectMessageType[SubmitResult]
      sequenceResult.submitResponse shouldBe a[Started]

      verify(script).executeNewSequenceHandler() // ESW-303: varifies newSequenceHandler is called
    }

    "not load and start executing a sequence if new sequence handler fails| ESW-303" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.failed(new RuntimeException) }

      val probe = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! SubmitSequenceInternal(sequence, probe.ref)

      assertSequenceNotStartedAndLoaded()
      probe.expectMessageType[NewSequenceHookFailed]

      verify(script).executeNewSequenceHandler() // ESW-303: varifies newSequenceHandler is called
    }

    "return Ok even if the processing of sequence fails | ESW-145, ESW-154, ESW-221, ESW-303" in {
      val sequence1      = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.idle(sequence1)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.successful(Done) }

      val client = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! SubmitSequenceInternal(sequence1, client.ref)

      val sequenceResult = client.expectMessageType[SubmitResult]
      sequenceResult.submitResponse shouldBe a[Started]
      val startedResponse = sequenceResult.toSubmitResponse()

      verify(script).executeNewSequenceHandler() // ESW-303: varifies newSequenceHandler is called
      assertSequencerState(InProgress)

      startPullNext()
      val errorMessage = "Some error"
      finishStepWithError(errorMessage)
      assertSequencerState(Idle)

      val qfProbe = TestProbe[SubmitResponse]()
      sequencerActor ! QueryFinal(startedResponse.runId, qfProbe.ref)
      qfProbe.expectMessage(Error(startedResponse.runId, errorMessage))
    }
  }

  "QueryFinal" must {
    "return Invalid response when sequencer is Idle and hasn't executed any sequence | ESW-221" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      val seqResProbe = TestProbe[SubmitResponse]()
      sequencerActor ! QueryFinal(Id(), seqResProbe.ref)
      seqResProbe.expectMessageType[Invalid]
    }

    "return Invalid response when sequencerId is invalid | ESW-221" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val seqResProbe = TestProbe[SubmitResponse]()
      val invalidId   = Id("invalid")
      sequencerActor ! QueryFinal(invalidId, seqResProbe.ref)
      seqResProbe.expectMessage(
        Invalid(invalidId, IdNotAvailableIssue(s"Sequencer is not running any sequence with runId $invalidId"))
      )
    }

    "return Sequence result with Completed when sequencer is in loaded state | ESW-145, ESW-154, ESW-221, ESW-303" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.successful(Done) }

      val startSeqProbe = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! StartSequence(startSeqProbe.ref)
      val sequenceResult  = startSeqProbe.expectMessageType[SequencerSubmitResponse]
      val startedResponse = sequenceResult.toSubmitResponse()
      startedResponse shouldBe a[Started]

      val seqResProbe = TestProbe[SubmitResponse]()
      sequencerActor ! QueryFinal(startedResponse.runId, seqResProbe.ref)

      pullAllStepsAndAssertSequenceIsFinished()

      seqResProbe.expectMessage(Completed(startedResponse.runId))
    }

    "return Sequence result with Completed when sequencer is inProgress state | ESW-145, ESW-154, ESW-221, ESW-303" in {
      val sequence1      = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.loaded(sequence1)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.successful(Done) }

      val startSeqProbe = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! StartSequence(startSeqProbe.ref)
      val sequenceResult  = startSeqProbe.expectMessageType[SubmitResult]
      val startedResponse = sequenceResult.submitResponse
      startedResponse shouldBe a[Started]

      startPullNext()
      assertSequencerState(InProgress)

      val seqResProbe = TestProbe[SubmitResponse]()
      sequencerActor ! QueryFinal(startedResponse.runId, seqResProbe.ref)
      seqResProbe.expectNoMessage(maxWaitForExpectNoMessage)

      finishStepWithSuccess()
      assertSequenceIsFinished()

      seqResProbe.expectMessage(Completed(startedResponse.runId))
    }

    "return Sequence result with Completed when sequencer has finished executing a sequence | ESW-145, ESW-154, ESW-221" in {
      val (startedResponse, sequencerSetup) = SequencerTestSetup.finished(sequence)
      import sequencerSetup._
      val seqResProbe = TestProbe[SubmitResponse]()
      sequencerActor ! QueryFinal(startedResponse.runId, seqResProbe.ref)

      seqResProbe.expectMessage(Completed(startedResponse.runId))
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

      assertCurrentSequence(Some(StepList(expectedSteps)))
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

      assertCurrentSequence(Some(StepList(expectedSteps)))
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

      when(script.executeGoOffline()).thenReturn(Future.successful(Done))
      goOfflineAndAssertResponse(Ok)
      assertSequencerState(Offline)

      goOnlineAndAssertResponse(Ok, Future.successful(Done))
      assertSequencerState(Idle)
    }
  }

  "Add" must {
    "add commands when sequence is loaded | ESW-114" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = TestProbe[OkOrUnhandledResponse]()
      sequencerActor ! Add(List(command3), probe.ref)
      probe.expectMessage(Ok)

      val updatedSequence = Sequence(command1, command2, command3)
      assertCurrentSequence(StepList(updatedSequence))
    }

    "add commands when sequence is in progress | ESW-114" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = TestProbe[OkOrUnhandledResponse]()
      sequencerActor ! Add(List(command3), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        Some(
          StepList(List(Step(command1).copy(status = InFlight), Step(command2), Step(command3)))
        )
      )
    }
  }

  "Prepend" must {
    "add steps before first pending step in Loaded state | ESW-113" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = TestProbe[OkOrUnhandledResponse]()
      sequencerActor ! Prepend(List(command3), probe.ref)
      probe.expectMessage(Ok)

      val updatedSequence = Sequence(command3, command1, command2)
      assertCurrentSequence(StepList(updatedSequence))
    }

    "add steps before first pending step in InProgress state | ESW-113" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = TestProbe[OkOrUnhandledResponse]()
      sequencerActor ! Prepend(List(command3), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        Some(
          StepList(List(Step(command1).copy(status = InFlight), Step(command3), Step(command2)))
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
          List(Step(command1, Finished.Success, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
        )
      )

      assertCurrentSequence(beforePauseStepList)

      //Engine can execute next step as 1st step is completed
      assertEngineCanExecuteNext(isReadyToExecuteNext = true)

      pauseAndAssertResponse(Ok)

      val afterPauseStepList = Some(
        StepList(
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
      val expectedPausedSequence = Some(StepList(expectedPausedSteps))

      val expectedResumedSteps = List(
        Step(command1, Finished.Success, hasBreakpoint = false),
        Step(command2, Pending, hasBreakpoint = false)
      )
      val expectedResumedSequence = Some(StepList(expectedResumedSteps))

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

      val expectedSequence = Some(StepList(expectedSteps))

      val stepId1 = getSequence().get.steps.head.id

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

      val expectedSequence = Some(StepList(expectedSteps))

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
      val expectedSequence = Some(StepList(expectedSteps))

      val step2 = getSequence().get.steps(1)

      val deleteResProbe = TestProbe[GenericResponse]()
      sequencerActor ! Delete(step2.id, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }

    "delete steps when sequencer is InProgress | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, InFlight, hasBreakpoint = false))
      val expectedSequence = Some(StepList(expectedSteps))

      val step2 = getSequence().get.steps(1)

      val deleteResProbe = TestProbe[GenericResponse]()
      sequencerActor ! Delete(step2.id, deleteResProbe.ref)
      deleteResProbe.expectMessage(Ok)

      assertCurrentSequence(expectedSequence)
    }

    "cannot delete inFlight steps when sequencer is InProgress | ESW-112" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSteps    = List(Step(command1, InFlight, hasBreakpoint = false), Step(command2, Pending, hasBreakpoint = false))
      val expectedSequence = Some(StepList(expectedSteps))

      val step1 = getSequence().get.steps.head

      val deleteResProbe = TestProbe[GenericResponse]()
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

      val step1 = stepListResult.get.steps.head

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
        Some(StepList(List(Step(command1), Step(command2).copy(hasBreakpoint = true))))

      val step2 = getSequence().get.steps(1)

      addBreakpointAndAssertResponse(step2.id, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakPoint =
        Some(StepList(List(Step(command1), Step(command2).copy(hasBreakpoint = false))))

      removeBreakpointAndAssertResponse(step2.id, Ok)
      assertCurrentSequence(expectedSequenceAfterRemovingBreakPoint)
    }

    "add and delete breakpoint to/from provided id when step status is Pending and sequencer is in InProgress state | ESW-106, ESW-107" in {
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val expectedSequenceAfterAddingBreakpoint =
        Some(StepList(List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = true))))

      val step2 = getSequence().get.steps(1)

      addBreakpointAndAssertResponse(step2.id, Ok)
      assertCurrentSequence(expectedSequenceAfterAddingBreakpoint)

      val expectedSequenceAfterRemovingBreakpoint =
        Some(StepList(List(Step(command1).copy(status = InFlight), Step(command2).copy(hasBreakpoint = false))))

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
      assertCurrentSequence(Some(StepList(List(Step(command1, status = InFlight, hasBreakpoint = false)))))
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

    "stop given inProgress sequence when it is paused | ESW-138" in {
      val sequencerSetup = SequencerTestSetup.inProgressWithFirstCommandComplete(sequence)
      import sequencerSetup._

      pauseAndAssertResponse(Ok)
      assertEngineCanExecuteNext(isReadyToExecuteNext = false)

      stopAndAssertResponse(Ok, InProgress)
    }
  }

  "GoOnline" must {
    "go to Idle state when sequencer is Offline | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      goOnlineAndAssertResponse(Ok, Future.successful(Done))
      verify(script).executeGoOnline()
      assertSequencerState(Idle)
      // try loading a sequence to ensure sequencer is online
      loadSequenceAndAssertResponse(Ok)
    }

    "remain in Idle state when sequencer is already Idle and call the goOnline handler | ESW-287" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      goOnlineAndAssertResponse(Ok, Future.successful(Done))
      assertSequencerState(Idle)

      // verify handlers are not called
      verify(script).executeGoOnline()

      // try loading a sequence to ensure sequencer is online
      loadSequenceAndAssertResponse(Ok)
    }

    "remain in offline state if online handlers fail | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      goOnlineAndAssertResponse(GoOnlineHookFailed(), Future.failed(new RuntimeException("GoOnline Hook Failed")))
      verify(script).executeGoOnline()
      assertSequencerState(Offline)
    }
  }

  "GoOffline" must {
    "go to Offline state from Idle state | ESW-194, ESW-141" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      when(script.executeGoOffline()).thenReturn(Future.successful(Done))
      goOfflineAndAssertResponse(Ok)
      verify(script).executeGoOffline()
      assertSequencerState(Offline)
    }

    "remain in Offline state when sequencer is already Offline and call the goOffline handler | ESW-287" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      goOfflineAndAssertResponse(Ok)
      assertSequencerState(Offline)

      // verify handlers are not only called again
      verify(script, times(2)).executeGoOffline()
    }

    "clear history of the last executed sequence | ESW-194" in {
      val (_, sequencerSetup) = SequencerTestSetup.finished(sequence)
      import sequencerSetup._

      when(script.executeGoOffline()).thenReturn(Future.successful(Done))
      goOfflineAndAssertResponse(Ok)
      assertCurrentSequence(None)
    }

    "not go to Offline state if the offline handlers fail | ESW-194" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      when(script.executeGoOffline()).thenReturn(Future.failed(new RuntimeException("GoOffline Hook Failed")))
      goOfflineAndAssertResponse(GoOfflineHookFailed())
      verify(script).executeGoOffline()
      assertSequencerState(Idle)
    }
  }

  "ReadyToExecuteNext" must {
    "return Ok immediately when a new step is available for execution" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadAndStartSequenceThenAssertInProgress()

      val probe = TestProbe[Ok.type]()
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectMessage(Ok)
    }

    "wait till completion of current command" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._
      loadAndStartSequenceThenAssertInProgress()

      startPullNext()

      val probe = TestProbe[Ok.type]()
      sequencerActor ! ReadyToExecuteNext(probe.ref)

      probe.expectNoMessage(maxWaitForExpectNoMessage)

      finishStepWithSuccess()

      probe.expectMessage(Ok)
    }

    "wait till a sequence is started | ESW-303" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      when { script.executeNewSequenceHandler() }.thenAnswer { Future.successful(Done) }

      val probe = TestProbe[Ok.type]()
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      // start the sequence and assert Ok is sent to the readyToExecuteNext subscriber as soon as a step is ready
      sequencerActor ! StartSequence(TestProbe[SequencerSubmitResponse]().ref)
      probe.expectMessage(Ok)
    }

    "wait till next sequence is received if current sequence is finished" in {
      val (_, sequencerSetup) = SequencerTestSetup.finished(sequence)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]()
      sequencerActor ! ReadyToExecuteNext(probe.ref)
      probe.expectNoMessage(maxWaitForExpectNoMessage)

      loadAndStartSequenceThenAssertInProgress()
      probe.expectMessage(Ok)
    }

    "wait till next sequence is received if sequencer went offline" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]()
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

      val probe = TestProbe[Ok.type]()
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
      val (_, sequencerSetup) = SequencerTestSetup.finished(sequence)
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
    "update the given step with successful response | ESW-241" in {
      val sequence       = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val probe = TestProbe[OkOrUnhandledResponse]()
      sequencerActor ! StepSuccess(probe.ref)

      assertCurrentSequence(
        Some(StepList(List(Step(command1, Finished.Success, hasBreakpoint = false))))
      )
    }

    "update the given step with error response | ESW-241" in {
      val sequence       = Sequence(command1)
      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._

      val message = "some"
      val probe   = TestProbe[OkOrUnhandledResponse]()
      sequencerActor ! StepFailure(message, probe.ref)

      assertCurrentSequence(
        Some(StepList(List(Step(command1, Finished.Failure(message), hasBreakpoint = false))))
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
    "set and get log level for component name | ESW-183, ESW-127" in {
      // This will initialize LoggingState and set akkaLogLevel, slf4jLogLevel and defaultLevel
      // If LoggingState is not initialized then akkaLogLevel, slf4jLogLevel and defaultLevel are null and
      // serialization of LogMetadata will fail
      LoggingSystemFactory.forTestingOnly()

      val sequencerSetup = SequencerTestSetup.inProgress(sequence)
      import sequencerSetup._
      val logMetadataProbe = TestProbe[LogMetadata]()

      sequencerActor ! GetComponentLogMetadata(logMetadataProbe.ref)

      val logMetadata1 = logMetadataProbe.expectMessageType[LogMetadata]

      logMetadata1.componentLevel shouldBe INFO
      val initialMetadata = LogAdminUtil.getLogMetadata(Prefix(ESW, sequencerName))
      initialMetadata.componentLevel shouldBe INFO

      sequencerActor ! SetComponentLogLevel(DEBUG)
      sequencerActor ! GetComponentLogMetadata(logMetadataProbe.ref)

      val logMetadata2 = logMetadataProbe.expectMessageType[LogMetadata]
      logMetadata2.componentLevel shouldBe DEBUG

      // this verifies that log metadata is updated in LogAdminUtil
      val finalMetadata = LogAdminUtil.getLogMetadata(Prefix(ESW, sequencerName))
      finalMetadata.componentLevel shouldBe DEBUG
    }
  }

  "GetSequenceComponent" must {

    val testCases: TableFor2[String, SequencerTestSetup] = Table.apply(
      ("state", "sequencer setup"),
      (InProgress.entryName, SequencerTestSetup.inProgress(sequence)),
      (Idle.entryName, SequencerTestSetup.idle(sequence)),
      (Loaded.entryName, SequencerTestSetup.loaded(sequence)),
      (Offline.entryName, SequencerTestSetup.offline(sequence))
    )

    forAll(testCases) { (stateName, testSetup) =>
      s"get sequence component name in $stateName state | ESW-255" in {
        import testSetup._
        val sequenceComponentRef = TestProbe[AkkaLocation]()

        sequencerActor ! GetSequenceComponent(sequenceComponentRef.ref)

        assertForGettingSequenceComponent(sequenceComponentRef)
      }
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
      SubmitSequenceInternal(sequence, _),
      StepSuccess,
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
      SubmitSequenceInternal(sequence, _),
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
      SubmitSequenceInternal(sequence, _),
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
      StepSuccess
    )
  }

  "State Transition Test" must {
    "Idle -> Loaded | ESW-141" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      loadSequenceAndAssertResponse(Ok)
      assertSequencerState(Loaded)
    }

    "Idle -> InProgress | ESW-141" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      loadAndStartSequenceThenAssertInProgress()
      assertSequencerState(InProgress) // transition to InProgress
    }

    "Loaded -> Offline | ESW-141" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      when(script.executeGoOffline()).thenReturn(Future.successful(Done))
      goOfflineAndAssertResponse(Ok)
      assertSequencerState(Offline) // transition to offline
    }

    "Loaded -> InProgress | ESW-141" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      //start executing sequence
      when { script.executeNewSequenceHandler() }.thenAnswer { Future.successful(Done) }
      val replyTo = TestProbe[SequencerSubmitResponse]()
      sequencerActor ! StartSequence(replyTo.ref)
      startPullNext()

      assertSequencerState(InProgress) // transition to InProgress
    }

    "Loaded -> Idle | ESW-141" in {
      val sequencerSetup = SequencerTestSetup.loaded(sequence)
      import sequencerSetup._

      val probe = TestProbe[OkOrUnhandledResponse]()
      sequencerActor ! Reset(probe.ref)
      probe.expectMessage(Ok)

      assertSequencerState(Idle) // transition to Idle
    }

    "InProgress -> Idle | ESW-141" in {
      val sequencerSetup = SequencerTestSetup.idle(sequence)
      import sequencerSetup._

      loadAndStartSequenceThenAssertInProgress()
      assertSequencerState(InProgress) // Initial state InProgres

      pullAllStepsAndAssertSequenceIsFinished()
      assertSequencerState(Idle) // transition to Idle
    }

    "Offline -> Idle | ESW-141" in {
      val sequencerSetup = SequencerTestSetup.offline(sequence)
      import sequencerSetup._

      goOnlineAndAssertResponse(Ok, Future.successful(Done))
      assertSequencerState(Idle) // transition to Idle
    }

    "Offline -> Shutdown | ESW-141" in {
      // actor system is needed as Shutdown message terminates the actorSystem of Sequencer
      val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val sequencerSetup                             = SequencerTestSetup.offline(sequence)(system)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]()(system)
      sequencerActor ! Shutdown(probe.ref)
      probe.expectMessage(Ok)
      probe.expectTerminated(sequencerActor)
    }

    "Loaded -> Shutdown | ESW-141" in {
      // actor system is needed as Shutdown message terminates the actorSystem of Sequencer
      val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val sequencerSetup                             = SequencerTestSetup.loaded(sequence)(system)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]()(system)
      sequencerActor ! Shutdown(probe.ref)
      probe.expectMessage(Ok)
      probe.expectTerminated(sequencerActor)
    }

    "InProgress -> Shutdown | ESW-141" in {
      // actor system is needed as Shutdown message terminates the actorSystem of Sequencer
      val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val sequencerSetup                             = SequencerTestSetup.inProgress(sequence)(system)
      import sequencerSetup._

      val probe = TestProbe[Ok.type]()(system)
      sequencerActor ! Shutdown(probe.ref)
      probe.expectMessage(Ok)
      probe.expectTerminated(sequencerActor)
    }
  }
}
