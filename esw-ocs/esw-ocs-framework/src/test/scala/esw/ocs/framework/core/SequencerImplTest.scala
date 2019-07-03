package esw.ocs.framework.core

import java.util.concurrent.CountDownLatch

import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.async.macros.StrandEc
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.StepStatus.InFlight
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

      processResponse.futureValue.right.value shouldBe cmd2Response
      sequencer.getSequence.futureValue.isFinished shouldBe true
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
      sequencer.pullNext().futureValue.right.value shouldBe Step(command1, InFlight, hasBreakpoint = false)

      sequencer.mayBeNext.futureValue shouldBe None
    }
  }
}
