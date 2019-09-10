package esw.ocs.impl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.{GoOnlineHookFailed, Unhandled}
import esw.ocs.api.protocol.{GoOnlineHookFailed, Ok, Unhandled}
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState.{Idle, InProgress, Loaded, Offline}

class SequencerAdminImplTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val command = Setup(Prefix("esw.test"), CommandName("command-1"), None)

  private val getSequenceResponse      = StepList(Sequence(command)).toOption
  private val getStateResponse         = Loaded
  private val addResponse              = Ok
  private val pauseResponse            = CannotOperateOnAnInFlightOrFinishedStep
  private val prependResponse          = Unhandled(Offline.entryName, "Prepend")
  private val resumeResponse           = Unhandled(Idle.entryName, "Resume")
  private val removeBreakpointResponse = IdDoesNotExist(Id())
  private val replaceResponse          = CannotOperateOnAnInFlightOrFinishedStep
  private val insertAfterResponse      = Ok
  private val resetResponse            = Ok
  private val abortResponse            = Unhandled(InProgress.entryName, "AbortSequence")
  private val deleteResponse           = IdDoesNotExist(Id())
  private val addBreakpointResponse    = Unhandled(Idle.entryName, "AddBreakpoint")
  private val goOnlineResponse         = GoOnlineHookFailed
  private val goOfflineResponse        = Unhandled(Offline.entryName, "Offline")

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
        case AbortSequence(replyTo)                                 => replyTo ! abortResponse
        case AddBreakpoint(`command`.runId, replyTo)                => replyTo ! addBreakpointResponse
        case RemoveBreakpoint(`command`.runId, replyTo)             => replyTo ! removeBreakpointResponse
        case GoOnline(replyTo)                                      => replyTo ! goOnlineResponse
        case GoOffline(replyTo)                                     => replyTo ! goOfflineResponse
        case _                                                      =>
      }
      Behaviors.same
    }

  private val sequencer = spawn(mockedBehavior)

  private val sequencerAdmin = new SequencerAdminImpl(sequencer)

  "getSequence" in {
    sequencerAdmin.getSequence.futureValue should ===(getSequenceResponse)
  }

  "isAvailable" in {
    sequencerAdmin.isAvailable.futureValue should ===(false)
  }

  "isOnline" in {
    sequencerAdmin.isOnline.futureValue should ===(true)
  }

  "add" in {
    sequencerAdmin.add(List(command)).futureValue should ===(addResponse)
  }

  "prepend" in {
    sequencerAdmin.prepend(List(command)).futureValue should ===(prependResponse)
  }

  "replace" in {
    sequencerAdmin.replace(command.runId, List(command)).futureValue should ===(replaceResponse)
  }

  "insertAfter" in {
    sequencerAdmin.insertAfter(command.runId, List(command)).futureValue should ===(insertAfterResponse)
  }

  "delete" in {
    sequencerAdmin.delete(command.runId).futureValue should ===(deleteResponse)
  }

  "pause" in {
    sequencerAdmin.pause.futureValue should ===(pauseResponse)
  }

  "resume" in {
    sequencerAdmin.resume.futureValue should ===(resumeResponse)
  }

  "addBreakpoint" in {
    sequencerAdmin.addBreakpoint(command.runId).futureValue should ===(addBreakpointResponse)
  }

  "removeBreakpoint" in {
    sequencerAdmin.removeBreakpoint(command.runId).futureValue should ===(removeBreakpointResponse)
  }

  "reset" in {
    sequencerAdmin.reset().futureValue should ===(resetResponse)
  }

  "abortSequence" in {
    sequencerAdmin.abortSequence().futureValue should ===(abortResponse)
  }
}
