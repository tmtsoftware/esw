package esw.ocs.core

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.StepList
import esw.ocs.api.models.StepStatus.Pending
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.EditorError._
import esw.ocs.api.models.messages.SequencerResponses.EditorResponse

class SequenceEditorClientTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val command      = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val status       = Pending
  private val notAllowed   = Left(NotAllowedOnFinishedSeq)
  private val notSupported = Left(NotSupported(status))
  private val pauseFailed  = Left(PauseFailed("failed to pause"))

  private val getSequenceResponse      = StepList(Sequence(command)).toOption.get
  private val availableResponse        = true
  private val addResponse              = EditorResponse(notAllowed)
  private val pauseResponse            = EditorResponse(pauseFailed)
  private val prependResponse          = EditorResponse(notAllowed)
  private val resumeResponse           = EditorResponse(notAllowed)
  private val removeBreakpointResponse = EditorResponse(notAllowed)
  private val resetResponse            = EditorResponse(notAllowed)
  private val replaceResponse          = EditorResponse(notSupported)
  private val insertAfterResponse      = EditorResponse(notSupported)
  private val deleteResponse           = EditorResponse(notSupported)
  private val addBreakpointResponse    = EditorResponse(notSupported)

  private val mockedBehavior: Behaviors.Receive[ExternalEditorMsg] =
    Behaviors.receiveMessage[ExternalEditorMsg] { msg =>
      msg match {
        case GetSequence(replyTo)                                   => replyTo ! getSequenceResponse
        case Available(replyTo)                                     => replyTo ! availableResponse
        case Add(List(`command`), replyTo)                          => replyTo ! addResponse
        case Prepend(List(`command`), replyTo)                      => replyTo ! prependResponse
        case Replace(`command`.runId, List(`command`), replyTo)     => replyTo ! replaceResponse
        case InsertAfter(`command`.runId, List(`command`), replyTo) => replyTo ! insertAfterResponse
        case Delete(`command`.runId, replyTo)                       => replyTo ! deleteResponse
        case Pause(replyTo)                                         => replyTo ! pauseResponse
        case Resume(replyTo)                                        => replyTo ! resumeResponse
        case AddBreakpoint(`command`.runId, replyTo)                => replyTo ! addBreakpointResponse
        case RemoveBreakpoint(`command`.runId, replyTo)             => replyTo ! removeBreakpointResponse
        case Reset(replyTo)                                         => replyTo ! resetResponse
        case _                                                      =>
      }
      Behaviors.same
    }

  private val sequencer = spawn(mockedBehavior)

  private val sequenceEditorClient = new SequenceEditorClient(sequencer)

  "status" in {
    sequenceEditorClient.status.futureValue should ===(getSequenceResponse)
  }

  "isAvailable" in {
    sequenceEditorClient.isAvailable.futureValue should ===(availableResponse)
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
