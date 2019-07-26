package esw.ocs.core

import java.util.concurrent.CountDownLatch

import akka.Done
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.SequenceError.ExistingSequenceIsInProcess
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.macros.StrandEc
import org.mockito.Mockito.{never, verify, when}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Success, Try}

class SequencerTest extends BaseTestSuite {
  implicit val timeout: Timeout = Timeout(10.seconds)

  class SequencerSetup(sequence: Sequence) {
    val crmMock: CommandResponseManager = mock[CommandResponseManager]

    implicit val strandEc: StrandEc   = StrandEc()
    implicit val ec: ExecutionContext = strandEc.ec

    val completionPromise: Promise[SubmitResponse] = Promise[SubmitResponse]()
    when(crmMock.queryFinal(sequence.runId)).thenReturn(completionPromise.future)

    val sequencer = new Sequencer(crmMock)

    def queryResponse(submitResponse: SubmitResponse, latch: CountDownLatch) = Future {
      latch.countDown()
      if (latch.getCount == 0) completionPromise.complete(Success(submitResponse))
      submitResponse
    }
  }

  "load" must {
    "store steplist in intermediate result | ESW-154" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val loadResponse = sequencer.load(sequence)
      loadResponse.futureValue.response.rightValue should ===(Done)
      sequencer.getSequence.futureValue.isEmpty should ===(true)
    }
  }

  "start" must {
    "execute existing loaded sequence | ESW-154" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))
      val latch    = new CountDownLatch(1)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))

      val loadResponse = sequencer.load(sequence)
      loadResponse.futureValue.response.rightValue should ===(Done)

      val sequenceResponse = sequencer.start()

      val pulled1 = sequencer.pullNext().futureValue
      pulled1.command should ===(command1)

      sequenceResponse.futureValue.response.rightValue should ===(Completed(sequence.runId))
    }
  }

  "loadAndStartSequence" must {
    "get completed sequence response when all commands succeed | ESW-158, ESW-145" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))
      val latch    = new CountDownLatch(2)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      val cmd2Response = Completed(command2.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))
      when(crmMock.queryFinal(command2.runId)).thenAnswer(_ => queryResponse(cmd2Response, latch))

      val sequenceResponse = sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val pulled1F = sequencer.pullNext()
      val pulled2F = sequencer.pullNext()
      val pulled1  = pulled1F.futureValue
      val pulled2  = pulled2F.futureValue

      pulled1.command should ===(command1)
      pulled2.command should ===(command2)

      sequenceResponse.futureValue.response.rightValue should ===(Completed(sequence.runId))
      val finalResp = sequencer.getSequence.futureValue
      finalResp.isFinished should ===(true)
    }

    "short circuit on first failed command and get failed sequence response | ESW-158, ESW-145" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val command3 = Observe(Prefix("test"), CommandName("command-3"), None)
      val command4 = Observe(Prefix("test"), CommandName("command-4"), None)
      val sequence = Sequence(Id(), Seq(command1, command2, command3, command4))
      val latch    = new CountDownLatch(2)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      val cmd2Response = Cancelled(command2.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))
      when(crmMock.queryFinal(command2.runId)).thenAnswer(_ => queryResponse(cmd2Response, latch))

      val processResponse = sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val pulled1 = sequencer.pullNext().futureValue
      val pulled2 = sequencer.pullNext().futureValue

      pulled1.command should ===(command1)
      pulled2.command should ===(command2)

      processResponse.futureValue.response.rightValue should ===(Cancelled(sequence.runId))

      sequencer.getSequence.futureValue.isFinished should ===(true)
      sequencer.isAvailable.futureValue should ===(true)
      verify(crmMock, never()).queryFinal(command3.runId)
      verify(crmMock, never()).queryFinal(command4.runId)
    }

    "fail with ExistingSequenceIsInProcess error when existing sequence is not finished | ESW-158, ESW-145" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      sequencer.loadAndStart(sequence)
      eventually(sequencer.isAvailable.futureValue should ===(false))

      val command3    = Setup(Prefix("test"), CommandName("command-3"), None)
      val command4    = Observe(Prefix("test"), CommandName("command-4"), None)
      val newSequence = Sequence(Id(), Seq(command3, command4))

      val processResponse2 = sequencer.loadAndStart(newSequence)
      processResponse2.futureValue.response.leftValue should ===(ExistingSequenceIsInProcess)
    }
  }

  "readyToExecuteNext" must {
    "return Done immediately when command is available for execution" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      sequencer.readyToExecuteNext().futureValue shouldBe Done
    }
    "wait till completion of current command" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)

      val p = Promise[SubmitResponse]
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => p.future)

      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      sequencer.pullNext()
      val readyToExecuteNextF = sequencer.readyToExecuteNext()
      readyToExecuteNextF.value should ===(None)

      // this will complete command1
      p.complete(Try(cmd1Response))
      readyToExecuteNextF.futureValue should ===(Done)

    }
    "wait till next sequence is received if current sequence is finished" in {
      val command1       = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence       = Sequence(Id(), Seq(command1))
      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)

      val p = Promise[SubmitResponse]
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => p.future)

      val sequence1Response = sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      sequencer.pullNext().futureValue
      val readyToExecuteNextF = sequencer.readyToExecuteNext()
      readyToExecuteNextF.value should ===(None)

      p.complete(Try(cmd1Response))                                  // this will complete command1
      completionPromise.complete(Success(Completed(sequence.runId))) // this will complete the sequence
      sequence1Response.futureValue.response.rightValue should ===(Completed(sequence.runId))

      readyToExecuteNextF.value should ===(None)

      val command2    = Setup(Prefix("test"), CommandName("command-2"), None)
      val newSequence = Sequence(Id(), Seq(command2))
      sequencer.loadAndStart(newSequence)
      readyToExecuteNextF.futureValue should ===(Done)
    }
  }

  "mayBeNext" must {
    "return next pending command" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      sequencer.mayBeNext.futureValue.value should ===(Step(command1))
    }

    "not return any command when no command is in Pending status" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      val latch = new CountDownLatch(1)

      val cmd1Response = Completed(command1.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))
      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      sequencer.pullNext().futureValue should ===(Step(command1, InFlight, hasBreakpoint = false))

      sequencer.mayBeNext.futureValue should ===(None)
    }
  }

  "add" must {
    "add provided list of commands to existing sequence | ESW-114" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val command4 = Observe(Prefix("test"), CommandName("command-4"), None)

      sequencer.add(List(command3, command4)).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(
        StepList(
          sequence.runId,
          List(Step(command1), Step(command2), Step(command3), Step(command4))
        )
      )

    }
  }

  "pause" must {
    "pause at next pending command from the sequence | ESW-104" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))
      val latch    = new CountDownLatch(2)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))

      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val pulled1 = sequencer.pullNext().futureValue
      val step1   = Step(command1, InFlight, hasBreakpoint = false)
      pulled1 should ===(step1)

      sequencer.pause.rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(
        StepList(
          sequence.runId,
          List(
            step1.copy(status = Finished.Success(cmd1Response)),
            Step(command2, Pending, hasBreakpoint = true)
          )
        )
      )
    }
  }

  "resume" must {
    "resume executing sequence | ESW-105" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))
      val latch    = new CountDownLatch(2)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))

      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val pulled1 = sequencer.pullNext().futureValue
      val step1   = Step(command1, InFlight, hasBreakpoint = false)
      pulled1 should ===(step1)

      sequencer.pause.rightValue should ===(Done)

      sequencer.resume.rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(
        StepList(
          sequence.runId,
          List(
            step1.copy(status = Finished.Success(cmd1Response)),
            Step(command2, Pending, hasBreakpoint = false)
          )
        )
      )
    }
  }

  "discardPending" must {
    "remove all the pending commands from sequence | ESW-110" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      sequencer.reset().rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(StepList(sequence.runId, Nil))
    }
  }

  "replace" must {
    "replace step matching provided id with given list of commands | ESW-108" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(Id(), Seq(command1, command2, command3))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val command4 = Setup(Prefix("test"), CommandName("command-4"), None)
      val command5 = Observe(Prefix("test"), CommandName("command-5"), None)
      val expectedReplacedStepList =
        StepList(sequence.runId, List(Step(command1), Step(command4), Step(command5), Step(command3)))
      sequencer.replace(command2.runId, List(command4, command5)).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedReplacedStepList)
    }
  }

  "prepend" must {
    "prepend provided list of commands to sequence | ESW-113" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val command4 = Observe(Prefix("test"), CommandName("command-4"), None)
      val expectedPrependedStepList =
        StepList(sequence.runId, List(Step(command3), Step(command4), Step(command1), Step(command2)))
      sequencer.prepend(List(command3, command4)).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedPrependedStepList)
    }
  }

  "delete" must {
    "delete step matching provided id | ESW-112" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val expectedDeletedStepList = StepList(sequence.runId, List(Step(command1)))
      sequencer.delete(command2.runId).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedDeletedStepList)
    }
  }

  "addBreakpoint & removeBreakpoint" must {
    "add and remove breakpoint at step matching provided id | ESW-106, ESW-107" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val breakpointAddedStepList = StepList(sequence.runId, List(Step(command1), Step(command2, Pending, hasBreakpoint = true)))
      sequencer.addBreakpoint(command2.runId).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(breakpointAddedStepList)

      val breakpointRemovedStepList =
        StepList(sequence.runId, List(Step(command1), Step(command2, Pending, hasBreakpoint = false)))
      sequencer.removeBreakpoint(command2.runId).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(breakpointRemovedStepList)
    }
  }

  "insertAfter" must {
    "insert provided list commands after matching step | ESW-111" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(Id(), Seq(command1, command2, command3))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      val command4 = Setup(Prefix("test"), CommandName("command-4"), None)
      val command5 = Observe(Prefix("test"), CommandName("command-5"), None)
      val expectedStepList =
        StepList(sequence.runId, List(Step(command1), Step(command2), Step(command4), Step(command5), Step(command3)))
      sequencer.insertAfter(command2.runId, List(command4, command5)).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedStepList)
    }
  }

  "previousSequence" must {
    "return old sequence after receiving new sequence | ESW-157" in {
      val command1  = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2  = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence  = Sequence(Id(), Seq(command1))
      val sequence2 = Sequence(Id(), Seq(command2))
      val latch     = new CountDownLatch(1)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      val cmd2Response = Completed(command2.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ => queryResponse(cmd1Response, latch))
      when(crmMock.queryFinal(command2.runId)).thenAnswer(_ => queryResponse(cmd2Response, latch))

      val processResponse = sequencer.loadAndStart(sequence)
      eventually(sequencer.getSequence.futureValue should ===(StepList(sequence).rightValue))

      sequencer.pullNext().futureValue
      processResponse.futureValue.response.rightValue should ===(Completed(sequence.runId))
      val currentSequence = sequencer.getSequence.futureValue

      sequencer.loadAndStart(sequence2)
      val previousSequence = sequencer.getPreviousSequence.futureValue

      previousSequence should ===(Some(currentSequence))
      // get completion responses for steps
      previousSequence.get.steps should ===(List(Step(command1, Finished.Success(cmd1Response), hasBreakpoint = false)))
    }
  }
}
