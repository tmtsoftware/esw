package esw.ocs.framework.core

import akka.actor.testkit.typed.scaladsl.ActorTestKitBase
import akka.actor.typed.scaladsl.Behaviors
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.Prefix
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.StepStatus.Pending
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.messages.error.StepListError._
import esw.ocs.framework.api.models.{Sequence, StepList}

class SequenceEditorClientTest extends ActorTestKitBase with BaseTestSuite {

  private val command      = Setup(Prefix("test"), CommandName("command-1"), None)
  private val status       = Pending
  private val notAllowed   = Left(NotAllowedOnFinishedSeq)
  private val notSupported = Left(NotSupported(status))

  private val getSequenceResponse      = StepList(Sequence(command)).toOption.get
  private val availableResponse        = true
  private val addResponse              = Left(AddFailed)
  private val pauseResponse            = Left(PauseFailed)
  private val prependResponse          = notAllowed
  private val resumeResponse           = notAllowed
  private val removeBreakpointResponse = notAllowed
  private val resetResponse            = notAllowed
  private val replaceResponse          = notSupported
  private val insertAfterResponse      = notSupported
  private val deleteResponse           = notSupported
  private val addBreakpointResponse    = notSupported

  private val mockedBehavior: Behaviors.Receive[ExternalSequencerMsg] = Behaviors.receiveMessage[ExternalSequencerMsg] { msg =>
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
