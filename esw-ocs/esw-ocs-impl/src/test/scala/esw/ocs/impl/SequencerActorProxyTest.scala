package esw.ocs.impl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.params.commands.CommandResponse.{Completed, Started}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.{GoOnlineHookFailed, Ok, SubmitResult, Unhandled}
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState.{Idle, Loaded, Offline}

class SequencerActorProxyTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val command             = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val getSequenceResponse = Some(StepList(Sequence(command)))
  private val stepId              = getSequenceResponse.get.steps.head.id
  private val sequenceId          = Id()
  private val sequence            = Sequence(command)
  private val startTime           = UTCTime.now()
  private val hint                = "engineering"

  private val getStateResponse         = Loaded
  private val addResponse              = Ok
  private val pauseResponse            = CannotOperateOnAnInFlightOrFinishedStep
  private val prependResponse          = Unhandled(Offline.entryName, "Prepend")
  private val resumeResponse           = Unhandled(Idle.entryName, "Resume")
  private val removeBreakpointResponse = IdDoesNotExist(Id())
  private val replaceResponse          = CannotOperateOnAnInFlightOrFinishedStep
  private val insertAfterResponse      = Ok
  private val resetResponse            = Ok
  private val abortResponse            = Unhandled(Loaded.entryName, "AbortSequence")
  private val stopResponse             = Unhandled(Loaded.entryName, "Stop")
  private val deleteResponse           = IdDoesNotExist(Id())
  private val addBreakpointResponse    = Unhandled(Idle.entryName, "AddBreakpoint")
  private val goOnlineResponse         = GoOnlineHookFailed
  private val goOfflineResponse        = Unhandled(Offline.entryName, "Offline")
  private val loadSequenceResponse     = Ok
  private val startSequenceResponse    = SubmitResult(Started(Id("runId1")))
  private val submitSequenceResponse   = SubmitResult(Started(Id("runId2")))
  private val diagnosticModeResponse   = Ok
  private val operationsModeResponse   = Ok
  private val queryFinalResponse       = Completed(Id())

  private val mockedBehavior: Behaviors.Receive[SequencerMsg] =
    Behaviors.receiveMessage[SequencerMsg] { msg =>
      msg match {
        case GetSequence(replyTo)                            => replyTo ! getSequenceResponse
        case GetSequencerState(replyTo)                      => replyTo ! getStateResponse
        case Add(List(`command`), replyTo)                   => replyTo ! addResponse
        case Prepend(List(`command`), replyTo)               => replyTo ! prependResponse
        case Replace(`stepId`, List(`command`), replyTo)     => replyTo ! replaceResponse
        case InsertAfter(`stepId`, List(`command`), replyTo) => replyTo ! insertAfterResponse
        case Delete(`stepId`, replyTo)                       => replyTo ! deleteResponse
        case Pause(replyTo)                                  => replyTo ! pauseResponse
        case Resume(replyTo)                                 => replyTo ! resumeResponse
        case Reset(replyTo)                                  => replyTo ! resetResponse
        case AbortSequence(replyTo)                          => replyTo ! abortResponse
        case Stop(replyTo)                                   => replyTo ! stopResponse
        case AddBreakpoint(`stepId`, replyTo)                => replyTo ! addBreakpointResponse
        case RemoveBreakpoint(`stepId`, replyTo)             => replyTo ! removeBreakpointResponse

        // commandApi
        case GoOnline(replyTo)                            => replyTo ! goOnlineResponse
        case GoOffline(replyTo)                           => replyTo ! goOfflineResponse
        case LoadSequence(`sequence`, replyTo)            => replyTo ! loadSequenceResponse
        case StartSequence(replyTo)                       => replyTo ! startSequenceResponse
        case SubmitSequenceInternal(`sequence`, replyTo)  => replyTo ! submitSequenceResponse
        case QueryFinal(_, replyTo)                       => replyTo ! queryFinalResponse
        case DiagnosticMode(`startTime`, `hint`, replyTo) => replyTo ! diagnosticModeResponse
        case OperationsMode(replyTo)                      => replyTo ! operationsModeResponse
        case _                                            => //
      }
      Behaviors.same
    }

  private val sequencerRef = spawn(mockedBehavior)

  private val sequencer    = new SequencerActorProxy(sequencerRef, Source.empty)

  "getSequence | ESW-222" in {
    sequencer.getSequence.futureValue should ===(getSequenceResponse)
  }

  "isAvailable | ESW-222" in {
    sequencer.isAvailable.futureValue should ===(false)
  }

  "isOnline | ESW-222" in {
    sequencer.isOnline.futureValue should ===(true)
  }

  "add | ESW-222" in {
    sequencer.add(List(command)).futureValue should ===(addResponse)
  }

  "prepend | ESW-222" in {
    sequencer.prepend(List(command)).futureValue should ===(prependResponse)
  }

  "replace | ESW-222" in {
    sequencer.replace(stepId, List(command)).futureValue should ===(replaceResponse)
  }

  "insertAfter | ESW-222" in {
    sequencer.insertAfter(stepId, List(command)).futureValue should ===(insertAfterResponse)
  }

  "delete | ESW-222" in {
    sequencer.delete(stepId).futureValue should ===(deleteResponse)
  }

  "pause | ESW-222" in {
    sequencer.pause.futureValue should ===(pauseResponse)
  }

  "resume | ESW-222" in {
    sequencer.resume.futureValue should ===(resumeResponse)
  }

  "addBreakpoint | ESW-222" in {
    sequencer.addBreakpoint(stepId).futureValue should ===(addBreakpointResponse)
  }

  "removeBreakpoint | ESW-222" in {
    sequencer.removeBreakpoint(stepId).futureValue should ===(removeBreakpointResponse)
  }

  "reset | ESW-222" in {
    sequencer.reset().futureValue should ===(resetResponse)
  }

  "abortSequence | ESW-222" in {
    sequencer.abortSequence().futureValue should ===(abortResponse)
  }

  "stop | ESW-222" in {
    sequencer.stop().futureValue should ===(stopResponse)
  }

  // commandApi

  "loadSequence | ESW-101" in {
    sequencer.loadSequence(sequence).futureValue should ===(Ok)
  }

  "startSequence | ESW-101" in {
    sequencer.startSequence().futureValue should ===(startSequenceResponse.toSubmitResponse())
  }

  "submit | ESW-101" in {
    sequencer.submit(sequence).futureValue should ===(submitSequenceResponse.toSubmitResponse())
  }

  "submitAndWait | ESW-101" in {
    sequencer.submitAndWait(sequence).futureValue should ===(queryFinalResponse)
  }

  "queryFinal | ESW-101" in {
    sequencer.queryFinal(sequenceId).futureValue should ===(queryFinalResponse)
  }

  "diagnosticMode | ESW-143" in {
    sequencer.diagnosticMode(startTime, hint).futureValue should ===(diagnosticModeResponse)
  }

  "operationsMode | ESW-143" in {
    sequencer.operationsMode().futureValue should ===(operationsModeResponse)
  }
}
