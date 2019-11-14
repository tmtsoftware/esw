package esw.ocs.impl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.{Ok, Unhandled}
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState.{Idle, Loaded, Offline}

class SequencerAdminImplTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val command             = Setup(Prefix("esw.test"), CommandName("command-1"), None)
  private val getSequenceResponse = Some(StepList(Sequence(command)))
  private val stepId              = getSequenceResponse.get.steps.head.id

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
        case _                                               => //
      }
      Behaviors.same
    }

  private val sequencer = spawn(mockedBehavior)

  private val sequencerAdmin = new SequencerAdminImpl(sequencer)

  "getSequence | ESW-222" in {
    sequencerAdmin.getSequence.futureValue should ===(getSequenceResponse)
  }

  "isAvailable | ESW-222" in {
    sequencerAdmin.isAvailable.futureValue should ===(false)
  }

  "isOnline | ESW-222" in {
    sequencerAdmin.isOnline.futureValue should ===(true)
  }

  "add | ESW-222" in {
    sequencerAdmin.add(List(command)).futureValue should ===(addResponse)
  }

  "prepend | ESW-222" in {
    sequencerAdmin.prepend(List(command)).futureValue should ===(prependResponse)
  }

  "replace | ESW-222" in {
    sequencerAdmin.replace(stepId, List(command)).futureValue should ===(replaceResponse)
  }

  "insertAfter | ESW-222" in {
    sequencerAdmin.insertAfter(stepId, List(command)).futureValue should ===(insertAfterResponse)
  }

  "delete | ESW-222" in {
    sequencerAdmin.delete(stepId).futureValue should ===(deleteResponse)
  }

  "pause | ESW-222" in {
    sequencerAdmin.pause.futureValue should ===(pauseResponse)
  }

  "resume | ESW-222" in {
    sequencerAdmin.resume.futureValue should ===(resumeResponse)
  }

  "addBreakpoint | ESW-222" in {
    sequencerAdmin.addBreakpoint(stepId).futureValue should ===(addBreakpointResponse)
  }

  "removeBreakpoint | ESW-222" in {
    sequencerAdmin.removeBreakpoint(stepId).futureValue should ===(removeBreakpointResponse)
  }

  "reset | ESW-222" in {
    sequencerAdmin.reset().futureValue should ===(resetResponse)
  }

  "abortSequence | ESW-222" in {
    sequencerAdmin.abortSequence().futureValue should ===(abortResponse)
  }

  "stop | ESW-222" in {
    sequencerAdmin.stop().futureValue should ===(stopResponse)
  }
}
