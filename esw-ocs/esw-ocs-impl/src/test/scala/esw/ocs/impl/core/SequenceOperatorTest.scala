package esw.ocs.impl.core

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.{CommandName, Setup}
import csw.prefix.models.Prefix
import esw.ocs.api.actor.messages.SequencerMessages.*
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.{Step, StepStatus}
import esw.ocs.api.protocol.{Ok, PullNextResult}
import esw.testcommons.BaseTestSuite
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime

class SequenceOperatorTest extends BaseTestSuite {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")
  private val command                                             = Setup(Prefix("esw.test"), CommandName("command-1"), None)

  private val pullNextResponse   = PullNextResult(Step(command))
  private val mayBeNextResponse  = Some(Step(command))
  private val updateFailureProbe = TestProbe[StepStatus]()
  private val updateSuccessProbe = TestProbe[StepStatus]()

  private val mockedBehavior: Behaviors.Receive[SequencerMsg] = Behaviors.receiveMessage[SequencerMsg] { msg =>
    msg match {
      case PullNext(replyTo)           => replyTo ! pullNextResponse
      case MaybeNext(replyTo)          => replyTo ! mayBeNextResponse
      case ReadyToExecuteNext(replyTo) => replyTo ! Ok
      case StepSuccess(_)              => updateSuccessProbe.ref ! Finished.Success
      case StepFailure(message, _)     => updateFailureProbe.ref ! Finished.Failure(message)
      case _                           =>
    }
    Behaviors.same
  }

  private val sequencer        = system.systemActorOf(mockedBehavior, "sequencer-actor")
  private val sequenceOperator = new SequenceOperator(sequencer)

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  "pullNext" in {
    sequenceOperator.pullNext.futureValue should ===(pullNextResponse)
  }

  "maybeNext" in {
    sequenceOperator.maybeNext.futureValue should ===(mayBeNextResponse)
  }

  "readyToExecuteNext" in {
    sequenceOperator.readyToExecuteNext.futureValue should ===(Ok)
  }

  "updateFailure" in {
    sequenceOperator.stepFailure("Failed")
    updateFailureProbe.expectMessage(Finished.Failure("Failed"))
  }

}
