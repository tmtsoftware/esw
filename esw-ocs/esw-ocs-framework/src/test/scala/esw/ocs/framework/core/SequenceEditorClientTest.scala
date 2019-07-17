package esw.ocs.framework.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.ActorTestKitBase
import akka.actor.typed.scaladsl.Behaviors
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.Prefix
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.{Sequence, StepList}

import scala.concurrent.Future

class SequenceEditorClientTest extends ActorTestKitBase with BaseTestSuite {

  case class TestData(testName: String, method: () => Future[Any], expectedResponse: Any)

  private val command               = Setup(Prefix("test"), CommandName("command-1"), None)
  val getSequenceResponse: StepList = StepList(Sequence(command)).toOption.get
  val availableResponse: Boolean    = true
  private val addResponse           = Right(Done)

  private val mockedBehavior: Behaviors.Receive[ExternalSequencerMsg] = Behaviors.receiveMessage[ExternalSequencerMsg] { msg =>
    msg match {
      case GetSequence(replyTo)          => replyTo ! getSequenceResponse
      case Available(replyTo)            => replyTo ! availableResponse
      case Add(List(`command`), replyTo) => replyTo ! addResponse
      case _                             =>
    }
    Behaviors.same
  }

  private val sequencer = spawn(mockedBehavior)

  "SequenceEditorClient" must {

    val sequenceEditorClient = new SequenceEditorClient(sequencer)

    val testData = List(
      TestData("status", () => sequenceEditorClient.status, getSequenceResponse),
      TestData("isAvailable", () => sequenceEditorClient.isAvailable, availableResponse),
      TestData("add", () => sequenceEditorClient.add(List(command)), addResponse)
    )

    testData.foreach { test =>
      test.testName in {
        test.method().futureValue should ===(test.expectedResponse)
      }
    }
  }
}
