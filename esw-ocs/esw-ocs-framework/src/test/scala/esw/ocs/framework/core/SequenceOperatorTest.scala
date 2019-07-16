package esw.ocs.framework.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ActorTestKitBase, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import csw.params.commands.CommandResponse.Error
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.models.Prefix
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.Step
import esw.ocs.framework.api.models.messages.SequencerMsg
import esw.ocs.framework.api.models.messages.SequencerMsg.{MaybeNext, PullNext, ReadyToExecuteNext, UpdateFailure}

import scala.concurrent.Future

class SequenceOperatorTest extends ActorTestKitBase with BaseTestSuite {

  case class TestData(testName: String, method: () => Future[Any], expectedResponse: Any)

  private val command = Setup(Prefix("test"), CommandName("command-1"), None)

  private val pullNextResponse                               = Step(command)
  private val mayBeNextResponse                              = Some(Step(command))
  private val updateFailureProbe: TestProbe[CommandResponse] = TestProbe()

  private val mockedBehavior: Behaviors.Receive[SequencerMsg] = Behaviors.receiveMessage[SequencerMsg] { msg =>
    msg match {
      case PullNext(replyTo)             => replyTo ! pullNextResponse
      case MaybeNext(replyTo)            => replyTo ! mayBeNextResponse
      case ReadyToExecuteNext(replyTo)   => replyTo ! Done
      case UpdateFailure(submitResponse) => updateFailureProbe.ref ! submitResponse
    }
    Behaviors.same
  }

  private val sequencer        = spawn(mockedBehavior)
  private val sequenceOperator = new SequenceOperator(sequencer)

  private val testData: List[TestData] = List(
    TestData("pullNext", () => sequenceOperator.pullNext, pullNextResponse),
    TestData("maybeNext", () => sequenceOperator.maybeNext, mayBeNextResponse),
    TestData("readyToExecuteNext", () => sequenceOperator.readyToExecuteNext, Done)
  )

  "SequencerOperator" must {
    testData.foreach { test =>
      test.testName in {
        test.method().futureValue should ===(test.expectedResponse)
      }
    }

    "updateFailure" in {
      val response = Error(command.runId, "Failed")
      sequenceOperator.update(response)
      updateFailureProbe.expectMessage(response)
    }
  }

}
