package esw.ocs.framework.core

import java.util.concurrent.CountDownLatch

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
    "process sequence of commands" in {
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
      sequencer.getSequence.futureValue shouldBe StepList(sequence).right.value

      val pulled1 = sequencer.pullNext()
      val pulled2 = sequencer.pullNext()
      val res1    = pulled1.futureValue
      val res2    = pulled2.futureValue

      res1.right.value.command shouldBe command1
      res2.right.value.command shouldBe command2

      processResponse.rightValue shouldBe cmd2Response
      val finalResp = sequencer.getSequence.futureValue
      finalResp.isAvailable shouldBe true // sequence gets cleared on completion
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
      sequencer.mayBeNext.futureValue.value shouldBe Step(command1)
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
      sequencer.pullNext().rightValue shouldBe Step(command1, InFlight, hasBreakpoint = false)

      sequencer.mayBeNext.futureValue shouldBe None
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

      sequencer.add(List(command3, command4)).rightValue shouldBe
      StepList(sequence.runId, List(Step(command1), Step(command2), Step(command3), Step(command4)))
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
      res1.right.value shouldBe step1

      val pausedSequence = sequencer.pause.rightValue
      pausedSequence.isPaused shouldBe true
      pausedSequence shouldBe StepList(
        sequence.runId,
        List(
          step1.copy(status = Finished.Success(cmd1Response)),
          Step(command2, Pending, hasBreakpoint = true)
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
      res1.right.value shouldBe step1

      val pausedSequence = sequencer.pause.rightValue
      pausedSequence.isPaused shouldBe true

      val resumedSequence = sequencer.resume.rightValue
      resumedSequence.isPaused shouldBe false
      resumedSequence shouldBe StepList(
        sequence.runId,
        List(
          step1.copy(status = Finished.Success(cmd1Response)),
          Step(command2, Pending, hasBreakpoint = false)
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

      sequencer.discardPending.rightValue shouldBe StepList(sequence.runId, Nil)
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
      sequencer.replace(command2.runId, List(command4, command5)).rightValue shouldBe expectedReplacedStepList
      sequencer.getSequence.futureValue shouldBe expectedReplacedStepList
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
      sequencer.prepend(List(command3, command4)).rightValue shouldBe expectedPrependedStepList
      sequencer.getSequence.futureValue shouldBe expectedPrependedStepList
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
      sequencer.delete(command2.runId).rightValue shouldBe expectedDeletedStepList
      sequencer.getSequence.futureValue shouldBe expectedDeletedStepList
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
      sequencer.addBreakpoint(command2.runId).rightValue shouldBe breakpointAddedStepList
      sequencer.getSequence.futureValue shouldBe breakpointAddedStepList

      val breakpointRemovedStepList =
        StepList(sequence.runId, List(Step(command1), Step(command2, Pending, hasBreakpoint = false)))
      sequencer.removeBreakpoint(command2.runId).rightValue shouldBe breakpointRemovedStepList
      sequencer.getSequence.futureValue shouldBe breakpointRemovedStepList
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
      sequencer.insertAfter(command2.runId, List(command4, command5)).rightValue shouldBe expectedStepList
      sequencer.getSequence.futureValue shouldBe expectedStepList
    }
  }
}
