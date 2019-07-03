package esw.ocs.framework.core

import java.util.concurrent.CountDownLatch

import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.async.macros.StrandEc
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.{Sequence, StepList}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class SequencerImplTest extends BaseTestSuite with MockitoSugar {

  "Sequencer" must {
    "process sequence of commands" in {
      val crmMock = mock[CommandResponseManager]
      val latch   = new CountDownLatch(2)

      implicit val strandEc: StrandEc   = StrandEc()
      implicit val ec: ExecutionContext = strandEc.ec
      implicit val timeout: Timeout     = Timeout(10.seconds)
      val sequencerImpl                 = new SequencerImpl(crmMock)

      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Observe(Prefix("test"), CommandName("command-2"), None)
      val sequence = Sequence(Id(), Seq(command1, command2))

      val completionPromise = Promise[SubmitResponse]()
      when(crmMock.queryFinal(sequence.runId)).thenReturn(completionPromise.future)

      def queryResponse(submitResponse: SubmitResponse) = Future {
        latch.countDown()
        if (latch.getCount == 0) completionPromise.complete(Success(submitResponse))
        submitResponse
      }

      val cmd1Response = Completed(command1.runId)
      val cmd2Response = Completed(command2.runId)
      when(crmMock.queryFinal(command1.runId)).thenAnswer(_ ⇒ queryResponse(cmd1Response))
      when(crmMock.queryFinal(command2.runId)).thenAnswer(_ ⇒ queryResponse(cmd2Response))

      val processResponse = sequencerImpl.processSequence(sequence)
      sequencerImpl.getSequence.futureValue shouldBe StepList(sequence).right.value

      val pulled1 = sequencerImpl.pullNext()
      val pulled2 = sequencerImpl.pullNext()
      val res1    = pulled1.futureValue
      val res2    = pulled2.futureValue

      res1.right.value.command shouldBe command1
      res2.right.value.command shouldBe command2

      processResponse.futureValue.right.value shouldBe cmd2Response
      sequencerImpl.getSequence.futureValue.isFinished shouldBe true
    }
  }
}
