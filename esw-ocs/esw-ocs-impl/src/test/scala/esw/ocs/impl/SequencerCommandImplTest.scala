package esw.ocs.impl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.params.commands.CommandResponse.{Completed, Started}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.{GoOnlineHookFailed, Ok, SequenceResult, Unhandled}
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState.Offline

class SequencerCommandImplTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val command   = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val sequence  = Sequence(command)
  private val startTime = UTCTime.now()
  private val hint      = "engineering"

  private val goOnlineResponse       = GoOnlineHookFailed
  private val goOfflineResponse      = Unhandled(Offline.entryName, "Offline")
  private val loadSequenceResponse   = Ok
  private val startSequenceResponse  = SequenceResult(Started(Id("runId1")))
  private val submitSequenceResponse = SequenceResult(Started(Id("runId2")))
  private val diagnosticModeResponse = Ok
  private val operationsModeResponse = Ok
  private val queryFinalResponse     = Completed(Id())

  private val mockedBehavior: Behaviors.Receive[SequencerMsg] =
    Behaviors.receiveMessage[SequencerMsg] { msg =>
      msg match {

        case GoOnline(replyTo)                            => replyTo ! goOnlineResponse
        case GoOffline(replyTo)                           => replyTo ! goOfflineResponse
        case LoadSequence(`sequence`, replyTo)            => replyTo ! loadSequenceResponse
        case StartSequence(replyTo)                       => replyTo ! startSequenceResponse
        case SubmitSequence(`sequence`, replyTo)          => replyTo ! submitSequenceResponse
        case QueryFinal(replyTo)                          => replyTo ! queryFinalResponse
        case DiagnosticMode(`startTime`, `hint`, replyTo) => replyTo ! diagnosticModeResponse
        case OperationsMode(replyTo)                      => replyTo ! operationsModeResponse
        case _                                            => //
      }
      Behaviors.same
    }

  private val sequencer = spawn(mockedBehavior)

  private val sequencerCommandApi = new SequencerCommandImpl(sequencer)

  "loadSequence | ESW-101" in {
    sequencerCommandApi.loadSequence(sequence).futureValue should ===(Ok)
  }

  "startSequence | ESW-101" in {
    sequencerCommandApi.startSequence().futureValue should ===(startSequenceResponse.toSubmitResponse(sequence.runId))
  }

  "submit | ESW-101" in {
    sequencerCommandApi.submit(sequence).futureValue should ===(submitSequenceResponse.toSubmitResponse(sequence.runId))
  }

  "submitAndWait | ESW-101" in {
    sequencerCommandApi.submitAndWait(sequence).futureValue should ===(queryFinalResponse)
  }

  "queryFinal | ESW-101" in {
    sequencerCommandApi.queryFinal().futureValue should ===(queryFinalResponse)
  }

  "diagnosticMode | ESW-143" in {
    sequencerCommandApi.diagnosticMode(startTime, hint).futureValue should ===(diagnosticModeResponse)
  }

  "operationsMode | ESW-143" in {
    sequencerCommandApi.operationsMode().futureValue should ===(operationsModeResponse)
  }
}
