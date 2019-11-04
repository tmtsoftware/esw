package esw.ocs.impl.core

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.Prefix
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.{Step, StepStatus}
import esw.ocs.api.protocol.{Ok, PullNextResult}
import esw.ocs.impl.messages.SequencerMessages._

class SequenceOperatorImplTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val command = Setup(Prefix("esw.test"), CommandName("command-1"), None)

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

  private val sequencer        = spawn(mockedBehavior)
  private val sequenceOperator = new SequenceOperatorImpl(sequencer)

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
