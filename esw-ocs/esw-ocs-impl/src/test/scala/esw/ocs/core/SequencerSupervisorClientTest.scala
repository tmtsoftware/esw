package esw.ocs.core

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.sequencer.SequencerMsg
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.messages.Ok
import esw.ocs.api.models.messages.SequencerMessages.AbortSequence

class SequencerSupervisorClientTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val abortSequenceResponse: Ok.type = Ok

  private val mockedBehavior: Behaviors.Receive[SequencerMsg] =
    Behaviors.receiveMessage[SequencerMsg] { msg =>
      msg match {
        case AbortSequence(replyTo) => replyTo ! abortSequenceResponse
        case _                      =>
      }
      Behaviors.same
    }

  private val sequencer                 = spawn(mockedBehavior)
  private val sequencerSupervisorClient = new SequencerSupervisorClient(sequencer)

  "abortSequence" in {
    sequencerSupervisorClient.abortSequence().futureValue should ===(abortSequenceResponse)
  }

}
