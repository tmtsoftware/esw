package esw.ocs.core

import java.util.concurrent.CountDownLatch

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import akka.stream.TLSClientAuth
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.scaladsl.LocationService
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, CommandResponse, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepStatus.{Finished, InFlight}
import esw.ocs.api.models.messages.SequencerMessages.{Add, GetPreviousSequence, GetSequence, LoadSequence, StartSequence}
import esw.ocs.api.models.messages.{StepListResponse, _}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.dsl.Script
import org.mockito.Mockito.when

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class SequencerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  val command1  = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  val sequence1 = Sequence(Id(), Seq(command1))
  val command2  = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  val sequence2 = Sequence(Id(), Seq(command2))

  class SequencerSetup(sequence: Sequence) {
    val crm: CommandResponseManager   = mock[CommandResponseManager]
    implicit val ec: ExecutionContext = system.executionContext

    private val completionPromise = Promise[SubmitResponse]()
    when(crm.queryFinal(sequence.runId)).thenReturn(completionPromise.future)

    private val script                         = mock[Script]
    private val locationService                = mock[LocationService]
    private val componentId                    = ComponentId("sequencer1", ComponentType.Sequencer)
    private val sequencerBehavior              = new SequencerBehavior(componentId, script, locationService, crm)
    val sequencerActor: ActorRef[SequencerMsg] = spawn(sequencerBehavior.setup)

    def queryResponse(submitResponse: SubmitResponse, latch: CountDownLatch) = Future {
      latch.countDown()
      if (latch.getCount == 0) completionPromise.complete(Success(CommandResponse.withRunId(sequence.runId, submitResponse)))
      submitResponse
    }

    private def assertStepListResponse(
        expected: StepListResponse,
        msg: ActorRef[StepListResponse] => SequencerMsg
    ): Unit = {
      val probe: TestProbe[StepListResponse] = createTestProbe[StepListResponse]()
      sequencerActor ! msg(probe.ref)
      probe.expectMessage(expected)
    }

    def assertCurrentSequence(expected: StepListResponse): Unit =
      assertStepListResponse(expected, GetSequence)

    def assertPreviousSequence(expected: StepListResponse): Unit =
      assertStepListResponse(expected, GetPreviousSequence)

    def assertSequencerIsLoaded(sequence: Sequence, expected: LoadSequenceResponse): Unit = {
      val probe = createTestProbe[LoadSequenceResponse]()
      sequencerActor ! LoadSequence(sequence, probe.ref)
      probe.expectMessage(expected)
    }

    def assertSequencerIsInProgress(sequence: Sequence): TestProbe[SubmitResponse] = {
      val probe = createTestProbe[SubmitResponse]()
      sequencerActor ! LoadAndStartSequence(sequence, probe.ref)
      val p: TestProbe[StepListResponse] = createTestProbe[StepListResponse]()

      eventually {
        sequencerActor ! GetSequence(p.ref)
        val result = p.expectMessageType[StepListResult]
        result.stepList.isDefined shouldBe true
      }
      probe
    }
  }

  "LoadSequence" must {
    "load the given sequence in idle state" in {
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._
      assertSequencerIsLoaded(sequence1, Ok)
    }

    "fail when given sequence contains duplicate Ids" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))

      val sequencerSetup = new SequencerSetup(invalidSequence)
      import sequencerSetup._

      assertSequencerIsLoaded(invalidSequence, DuplicateIdsFound)
    }
  }

  "StartSequence" must {
    "start executing a sequence when sequencer is loaded" in {
      val latch          = new CountDownLatch(1)
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crm.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))

      assertSequencerIsLoaded(sequence1, Ok)

      val seqResProbe = createTestProbe[SequenceResponse]()
      sequencerActor ! StartSequence(seqResProbe.ref)
      seqResProbe.expectMessage(SequenceResult(Completed(sequence1.runId)))
    }
  }

  "LoadAndStartSequence" must {
    "load and process sequence in idle state" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))
      val latch    = new CountDownLatch(1)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crm.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))

      val probe = createTestProbe[SubmitResponse]()
      sequencerActor ! LoadAndStartSequence(sequence, probe.ref)
      probe.expectMessage(Completed(sequence.runId))
    }

    "fail when given sequence contains duplicate Ids" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))

      val sequencerSetup = new SequencerSetup(invalidSequence)
      import sequencerSetup._

      val loadSeqResProbe = createTestProbe[SubmitResponse]()
      sequencerActor ! LoadAndStartSequence(invalidSequence, loadSeqResProbe.ref)
      loadSeqResProbe.expectMessage(Error(invalidSequence.runId, DuplicateIdsFound.description))
    }
  }

  "GetSequence" must {
    val sequencerSetup = new SequencerSetup(sequence1)
    import sequencerSetup._

    "return None when in Idle state" in {
      assertCurrentSequence(StepListResult(None))
    }

    "return sequence when in loaded state" in {
      assertSequencerIsLoaded(sequence1, Ok)
      assertCurrentSequence(StepListResult(StepList(sequence1).toOption))
    }
  }

  "GetPreviousSequence" must {
    "return None when sequencer has not started executing any sequence" in {
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._
      // in idle state
      assertPreviousSequence(StepListResult(None))
      assertSequencerIsLoaded(sequence1, Ok)
      // in loaded state
      assertPreviousSequence(StepListResult(None))
    }

    "return previous sequence after new sequence is loaded" in {
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._

      val latch = new CountDownLatch(1)

      val cmd1Response = Completed(command1.runId)
      when(crm.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))

      val loadAndStartResProbe = createTestProbe[SubmitResponse]()
      sequencerActor ! LoadAndStartSequence(sequence1, loadAndStartResProbe.ref)
      loadAndStartResProbe.expectMessage(Completed(sequence1.runId))

      assertSequencerIsLoaded(sequence2, Ok)

      val expectedPreviousSequence = StepListResult(
        Some(StepList(sequence1.runId, List(Step(command1, Finished.Success(Completed(command1.runId)), hasBreakpoint = false))))
      )

      assertPreviousSequence(expectedPreviousSequence)
    }
  }

  "Add" must {
    "add commands when sequence is loaded" in {
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._

      assertSequencerIsLoaded(sequence1, Ok)
      val probe = createTestProbe[OkOrUnhandledResponse]()
      sequencerActor ! Add(List(command2), probe.ref)
      probe.expectMessage(Ok)

      val updatedSequence = sequence1.copy(commands = Seq(command1, command2))
      assertCurrentSequence(StepListResult(StepList(updatedSequence).toOption))
    }

    "add commands when sequence is in progress" in {
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._

      val submitResponsePromise = Promise[SubmitResponse]()
      when(crm.queryFinal(command1.runId)).thenReturn(submitResponsePromise.future)

      assertSequencerIsInProgress(sequence1)

      val probe = createTestProbe[OkOrUnhandledResponse]()
      sequencerActor ! Add(List(command2), probe.ref)
      probe.expectMessage(Ok)

      assertCurrentSequence(
        StepListResult(Some(StepList(sequence1.runId, List(Step(command1, InFlight, hasBreakpoint = false), Step(command2)))))
      )
    }
  }
}
