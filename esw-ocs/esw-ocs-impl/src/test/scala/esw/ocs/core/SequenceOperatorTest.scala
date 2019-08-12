//package esw.ocs.core
//
//import akka.Done
//import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
//import akka.actor.typed.scaladsl.Behaviors
//import csw.command.client.messages.sequencer.SequencerMsg
//import csw.params.commands.CommandResponse.Error
//import csw.params.commands.{CommandName, CommandResponse, Setup}
//import csw.params.core.models.Prefix
//import esw.ocs.api.BaseTestSuite
//import esw.ocs.api.models.Step
//import esw.ocs.api.models.messages.SequencerMessages.{MaybeNext, PullNext, ReadyToExecuteNext, UpdateFailure}
//
//class SequenceOperatorTest extends ScalaTestWithActorTestKit with BaseTestSuite {
//
//  private val command = Setup(Prefix("esw.test"), CommandName("command-1"), None)
//
//  private val pullNextResponse   = Step(command)
//  private val mayBeNextResponse  = Some(Step(command))
//  private val updateFailureProbe = TestProbe[CommandResponse]()
//
//  private val mockedBehavior: Behaviors.Receive[SequencerMsg] = Behaviors.receiveMessage[SequencerMsg] { msg =>
//    msg match {
//      case PullNext(replyTo)             => replyTo ! pullNextResponse
//      case MaybeNext(replyTo)            => replyTo ! mayBeNextResponse
//      case ReadyToExecuteNext(replyTo)   => replyTo ! Done
//      case UpdateFailure(submitResponse) => updateFailureProbe.ref ! submitResponse
//      case _                             =>
//    }
//    Behaviors.same
//  }
//
//  private val sequencer        = spawn(mockedBehavior)
//  private val sequenceOperator = new SequenceOperator(sequencer)
//
//  "pullNext" in {
//    sequenceOperator.pullNext.futureValue should ===(pullNextResponse)
//  }
//
//  "maybeNext" in {
//    sequenceOperator.maybeNext.futureValue should ===(mayBeNextResponse)
//  }
//
//  "readyToExecuteNext" in {
//    sequenceOperator.readyToExecuteNext.futureValue should ===(Done)
//  }
//
//  "updateFailure" in {
//    val response = Error(command.runId, "Failed")
//    sequenceOperator.update(response)
//    updateFailureProbe.expectMessage(response)
//  }
//
//}
