package esw.ocs.core

import java.util.concurrent.CountDownLatch

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.scaladsl.LocationService
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, SubmitResponse}
import csw.params.commands.{CommandName, CommandResponse, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.messages.SequencerMessages.{GetPreviousSequence, GetSequence, LoadSequence, StartSequence}
import esw.ocs.api.models.messages._
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
  }

  def assertSequencerIsLoaded(sequencerActor: ActorRef[SequencerMsg], sequence: Sequence): Unit = {
    val probe = createTestProbe[LoadSequenceResponse]()
    sequencerActor ! LoadSequence(sequence, probe.ref)
    probe.expectMessage(Ok)
  }

  "LoadSequence" must {
    "load the given sequence in idle state" in {
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._
      assertSequencerIsLoaded(sequencerActor, sequence1)
    }

    "fail when given sequence contains duplicate Ids" in {
      val invalidSequence = Sequence(Id(), Seq(command1, command1))

      val sequencerSetup = new SequencerSetup(invalidSequence)
      import sequencerSetup._

      val loadSeqResProbe = createTestProbe[LoadSequenceResponse]()
      sequencerActor ! LoadSequence(invalidSequence, loadSeqResProbe.ref)
      loadSeqResProbe.expectMessage(DuplicateIdsFound)
    }
  }

  "StartSequence" must {
    "start executing a sequence when sequencer is loaded" in {
      val latch          = new CountDownLatch(1)
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crm.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))

      assertSequencerIsLoaded(sequencerActor, sequence1)

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
    val command1       = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    val sequence       = Sequence(Id(), Seq(command1))
    val sequencerSetup = new SequencerSetup(sequence)
    import sequencerSetup._

    "return None when in Idle state" in {
      val probe = createTestProbe[StepListResponse]()
      sequencerActor ! GetSequence(probe.ref)
      probe.expectMessage(StepListResult(None))
    }

    "return sequence when in loaded state" in {
      val probe             = createTestProbe[StepListResponse]()
      val loadedSeqResProbe = createTestProbe[LoadSequenceResponse]
      sequencerActor ! LoadSequence(sequence, loadedSeqResProbe.ref)
      loadedSeqResProbe.expectMessage(Ok)
      sequencerActor ! GetSequence(probe.ref)
      probe.expectMessage(StepListResult(StepList(sequence).toOption))
    }
  }

  "GetPreviousSequence" must {
    "return None when sequencer has not started executing any sequence" in {
      val sequencerSetup = new SequencerSetup(sequence1)
      import sequencerSetup._

      val stepListResProbe = createTestProbe[StepListResponse]()
      sequencerActor ! GetPreviousSequence(stepListResProbe.ref)
      stepListResProbe.expectMessage(StepListResult(None))

      val loadedSeqResProbe = createTestProbe[LoadSequenceResponse]
      sequencerActor ! LoadSequence(sequence1, loadedSeqResProbe.ref)
      loadedSeqResProbe.expectMessage(Ok)

      sequencerActor ! GetPreviousSequence(stepListResProbe.ref)
      stepListResProbe.expectMessage(StepListResult(None))
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

      val loadedSeqResProbe = createTestProbe[LoadSequenceResponse]
      sequencerActor ! LoadSequence(sequence2, loadedSeqResProbe.ref)
      loadedSeqResProbe.expectMessage(Ok)

      val stepListResProbe = createTestProbe[StepListResponse]()
      sequencerActor ! GetPreviousSequence(stepListResProbe.ref)

      stepListResProbe.expectMessage(
        StepListResult(
          Some(
            StepList(sequence1.runId, List(Step(command1, Finished.Success(Completed(command1.runId)), hasBreakpoint = false)))
          )
        )
      )
    }
  }
}
