package esw.ocs.framework.core

import java.util.concurrent.CountDownLatch

import akka.Done
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.async.macros.StrandEc
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.framework.api.models.{Sequence, Step, StepList}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class SequencerImplTest extends BaseTestSuite with MockitoSugar {
  implicit val timeout: Timeout = Timeout(10.seconds)

  class SequencerSetup(sequence: Sequence) {
    val crmMock: CommandResponseManager = mock[CommandResponseManager]

    implicit val strandEc: StrandEc   = StrandEc()
    implicit val ec: ExecutionContext = strandEc.ec

    val completionPromise: Promise[SubmitResponse] = Promise[SubmitResponse]()
    when(crmMock.queryFinal(sequence.runId)).thenReturn(completionPromise.future)

    val sequencer = new SequencerImpl(crmMock)

    def queryResponse(submitResponse: SubmitResponse, latch: CountDownLatch) = Future {
      latch.countDown()
      if (latch.getCount == 0) completionPromise.complete(Success(submitResponse))
      submitResponse
    }
  }

  "Sequencer" must {
    "process sequence of commands when all the commands succeeds" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))
      val latch    = new CountDownLatch(2)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      val cmd2Response = Completed(command2.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ ⇒ queryResponse(cmd1Response, latch))
      when(crmMock.queryFinal(command2.runId)).thenAnswer(_ ⇒ queryResponse(cmd2Response, latch))

      val processResponse = sequencer.processSequence(sequence)
      sequencer.getSequence.futureValue should ===(StepList(sequence).right.value)

      val pulled1 = sequencer.pullNext()
      val pulled2 = sequencer.pullNext()
      val res1    = pulled1.futureValue
      val res2    = pulled2.futureValue

      res1.command should ===(command1)
      res2.command should ===(command2)

      processResponse.rightValue should ===(Completed(sequence.runId))
      val finalResp = sequencer.getSequence.futureValue
      finalResp.steps.isEmpty should ===(true) // sequence gets cleared on completion)
    }

    "process sequence of commands when one of the command fails" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val command3 = Observe(Prefix("test"), CommandName("command-3"), None)
      val command4 = Observe(Prefix("test"), CommandName("command-4"), None)
      val sequence = Sequence(Id(), Seq(command1, command2, command3, command4))
      val latch    = new CountDownLatch(3)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      val cmd2Response = Cancelled(command2.runId)
      val cmd3Response = Completed(command3.runId)
      val cmd4Response = Completed(command4.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ ⇒ queryResponse(cmd1Response, latch))
      when(crmMock.queryFinal(command2.runId)).thenAnswer(_ ⇒ queryResponse(cmd2Response, latch))
      when(crmMock.queryFinal(command3.runId)).thenAnswer(_ ⇒ queryResponse(cmd3Response, latch))
      when(crmMock.queryFinal(command3.runId)).thenAnswer(_ ⇒ queryResponse(cmd4Response, latch))

      val processResponse = sequencer.processSequence(sequence)
      sequencer.getSequence.futureValue should ===(StepList(sequence).right.value)

      val res1 = sequencer.pullNext().futureValue
      val res2 = sequencer.pullNext().futureValue

      res1.command should ===(command1)
      res2.command should ===(command2)

      processResponse.rightValue should ===(Cancelled(sequence.runId))
      val finalResp = sequencer.getSequence.futureValue
      finalResp.steps.isEmpty should ===(true) // sequence gets cleared on completion
    }
  }

  "mayBeNext" must {
    "return next pending command" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      sequencer.processSequence(sequence)
      sequencer.mayBeNext.futureValue.value should ===(Step(command1))
    }

    "not return any command when no command is in Pending status" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      val latch = new CountDownLatch(1)

      val cmd1Response = Completed(command1.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ ⇒ queryResponse(cmd1Response, latch))
      sequencer.processSequence(sequence)
      sequencer.pullNext().futureValue should ===(Step(command1, InFlight, hasBreakpoint = false))

      sequencer.mayBeNext.futureValue should ===(None)
    }
  }

  "add" must {
    "add provided list of commands to existing sequence" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      sequencer.processSequence(sequence)

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
    "pause at next pending command from the sequence" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))
      val latch    = new CountDownLatch(2)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ ⇒ queryResponse(cmd1Response, latch))

      sequencer.processSequence(sequence)

      val res1  = sequencer.pullNext().futureValue
      val step1 = Step(command1, InFlight, hasBreakpoint = false)
      res1 should ===(step1)

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
    "resume executing sequence" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))
      val latch    = new CountDownLatch(2)

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._

      val cmd1Response = Completed(command1.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ ⇒ queryResponse(cmd1Response, latch))

      sequencer.processSequence(sequence)

      val res1  = sequencer.pullNext().futureValue
      val step1 = Step(command1, InFlight, hasBreakpoint = false)
      res1 should ===(step1)

      val pausedSequence = sequencer.pause.rightValue
      pausedSequence should ===(Done)

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
    "remove all the pending commands from sequence" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.processSequence(sequence)

      sequencer.reset().rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(StepList(sequence.runId, Nil))
    }
  }

  "replace" must {
    "replace step matching provided id with given list of commands" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(Id(), Seq(command1, command2, command3))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.processSequence(sequence)

      val command4 = Setup(Prefix("test"), CommandName("command-4"), None)
      val command5 = Observe(Prefix("test"), CommandName("command-5"), None)
      val expectedReplacedStepList =
        StepList(sequence.runId, List(Step(command1), Step(command4), Step(command5), Step(command3)))
      sequencer.replace(command2.runId, List(command4, command5)).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedReplacedStepList)
    }
  }

  "prepend" must {
    "prepend provided list of commands to sequence" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.processSequence(sequence)

      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val command4 = Observe(Prefix("test"), CommandName("command-4"), None)
      val expectedPrependedStepList =
        StepList(sequence.runId, List(Step(command3), Step(command4), Step(command1), Step(command2)))
      sequencer.prepend(List(command3, command4)).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedPrependedStepList)
    }
  }

  "delete" must {
    "delete step matching provided id" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.processSequence(sequence)

      val expectedDeletedStepList = StepList(sequence.runId, List(Step(command1)))
      sequencer.delete(command2.runId).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedDeletedStepList)
    }
  }

  "addBreakpoint & removeBreakpoint" must {
    "add and remove breakpoint at step matching provided id" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.processSequence(sequence)

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
    "insert provided list commands after matching step" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(Id(), Seq(command1, command2, command3))

      val sequencerSetup = new SequencerSetup(sequence)
      import sequencerSetup._
      sequencer.processSequence(sequence)

      val command4 = Setup(Prefix("test"), CommandName("command-4"), None)
      val command5 = Observe(Prefix("test"), CommandName("command-5"), None)
      val expectedStepList =
        StepList(sequence.runId, List(Step(command1), Step(command2), Step(command4), Step(command5), Step(command3)))
      sequencer.insertAfter(command2.runId, List(command4, command5)).rightValue should ===(Done)
      sequencer.getSequence.futureValue should ===(expectedStepList)
    }
  }
}
