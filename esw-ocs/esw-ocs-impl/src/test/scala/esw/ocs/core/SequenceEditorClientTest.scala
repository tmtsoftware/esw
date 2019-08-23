package esw.ocs.core

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.SequencerState.{Idle, Loaded, Offline}
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.{Ok, Unhandled}

class SequenceEditorClientTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val command = Setup(Prefix("esw.test"), CommandName("command-1"), None)

  private val getSequenceResponse      = StepList(Sequence(command)).toOption
  private val getStateResponse         = Loaded
  private val addResponse              = Ok
  private val pauseResponse            = CannotOperateOnAnInFlightOrFinishedStep
  private val prependResponse          = Unhandled(Offline, "Prepend")
  private val resumeResponse           = Unhandled(Idle, "Resume")
  private val removeBreakpointResponse = IdDoesNotExist(Id())
  private val replaceResponse          = CannotOperateOnAnInFlightOrFinishedStep
  private val insertAfterResponse      = Ok
  private val resetResponse            = Ok
  private val deleteResponse           = IdDoesNotExist(Id())
  private val addBreakpointResponse    = Unhandled(Idle, "AddBreakpoint")

  private val mockedBehavior: Behaviors.Receive[SequencerMsg] =
    Behaviors.receiveMessage[SequencerMsg] { msg =>
      msg match {
        case GetSequence(replyTo)                                   => replyTo ! getSequenceResponse
        case GetSequencerState(replyTo)                             => replyTo ! getStateResponse
        case Add(List(`command`), replyTo)                          => replyTo ! addResponse
        case Prepend(List(`command`), replyTo)                      => replyTo ! prependResponse
        case Replace(`command`.runId, List(`command`), replyTo)     => replyTo ! replaceResponse
        case InsertAfter(`command`.runId, List(`command`), replyTo) => replyTo ! insertAfterResponse
        case Delete(`command`.runId, replyTo)                       => replyTo ! deleteResponse
        case Pause(replyTo)                                         => replyTo ! pauseResponse
        case Resume(replyTo)                                        => replyTo ! resumeResponse
        case Reset(replyTo)                                         => replyTo ! resetResponse
        case AddBreakpoint(`command`.runId, replyTo)                => replyTo ! addBreakpointResponse
        case RemoveBreakpoint(`command`.runId, replyTo)             => replyTo ! removeBreakpointResponse
        case _                                                      =>
      }
      Behaviors.same
    }

  private val sequencer = spawn(mockedBehavior)

  private val sequenceEditorClient = new SequenceEditorClient(sequencer)

  "getSequence" in {
    sequenceEditorClient.getSequence.futureValue should ===(getSequenceResponse)
  }

  "getState" in {
    sequenceEditorClient.getState.futureValue should ===(getStateResponse)
  }

  "add" in {
    sequenceEditorClient.add(List(command)).futureValue should ===(addResponse)
  }

  "prepend" in {
    sequenceEditorClient.prepend(List(command)).futureValue should ===(prependResponse)
  }

  "replace" in {
    sequenceEditorClient.replace(command.runId, List(command)).futureValue should ===(replaceResponse)
  }

  "insertAfter" in {
    sequenceEditorClient.insertAfter(command.runId, List(command)).futureValue should ===(insertAfterResponse)
  }

  "delete" in {
    sequenceEditorClient.delete(command.runId).futureValue should ===(deleteResponse)
  }

  "pause" in {
    sequenceEditorClient.pause.futureValue should ===(pauseResponse)
  }

  "resume" in {
    sequenceEditorClient.resume.futureValue should ===(resumeResponse)
  }

  "addBreakpoint" in {
    sequenceEditorClient.addBreakpoint(command.runId).futureValue should ===(addBreakpointResponse)
  }

  "removeBreakpoint" in {
    sequenceEditorClient.removeBreakpoint(command.runId).futureValue should ===(removeBreakpointResponse)
  }

  "reset" in {
    sequenceEditorClient.reset().futureValue should ===(resetResponse)
  }
}
