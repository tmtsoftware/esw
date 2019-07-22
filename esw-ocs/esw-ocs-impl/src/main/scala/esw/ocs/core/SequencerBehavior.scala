package esw.ocs.core

import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.{ProcessSequence, SequencerMsg}
import esw.ocs.api.models.messages.{EditorResponse, StepListResponse}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.error.{SequencerAbortError, SequencerShutdownError}
import esw.ocs.dsl.ScriptDsl

object SequencerBehavior {
  def behavior(sequencer: Sequencer, script: ScriptDsl): Behaviors.Receive[SequencerMsg] =
    Behaviors.receive[SequencerMsg] { (ctx, msg) =>
      import ctx.executionContext

      msg match {
        // ===== External Lifecycle =====
        case Shutdown(replyTo) =>
          script
            .executeShutdown()
            .map(Right(_))
            .recover { case ex: Exception => Left(SequencerShutdownError(ex.getMessage)) } // fixme: use NonFatal
            .foreach(replyTo ! EditorResponse(_))

        case Abort(replyTo) =>
          script
            .executeAbort()
            .map(Right(_))
            .recover { case ex: Exception => Left(SequencerAbortError(ex.getMessage)) }
            .foreach(replyTo ! EditorResponse(_))

        // ===== External Editor =====
        case ProcessSequence(sequence, replyTo) => sequencer.processSequence(sequence).foreach(replyTo.tell)
        case Available(replyTo)                 => sequencer.isAvailable.foreach(replyTo.tell)
        case GetSequence(replyTo)               => sequencer.getSequence.foreach(replyTo.tell)
        case GetPreviousSequence(replyTo)       => sequencer.getPreviousSequence.foreach(replyTo ! StepListResponse(_))
        case Add(commands, replyTo)             => sequencer.add(commands).foreach(replyTo ! EditorResponse(_))
        case Pause(replyTo)                     => sequencer.pause.foreach(replyTo ! EditorResponse(_))
        case Resume(replyTo)                    => sequencer.resume.foreach(replyTo ! EditorResponse(_))
        case Reset(replyTo)                     => sequencer.reset().foreach(replyTo ! EditorResponse(_))
        case Replace(id, commands, replyTo)     => sequencer.replace(id, commands).foreach(replyTo ! EditorResponse(_))
        case Prepend(commands, replyTo)         => sequencer.prepend(commands).foreach(replyTo ! EditorResponse(_))
        case Delete(id, replyTo)                => sequencer.delete(id).foreach(replyTo ! EditorResponse(_))
        case InsertAfter(id, commands, replyTo) => sequencer.insertAfter(id, commands).foreach(replyTo ! EditorResponse(_))
        case AddBreakpoint(id, replyTo)         => sequencer.addBreakpoint(id).foreach(replyTo ! EditorResponse(_))
        case RemoveBreakpoint(id, replyTo)      => sequencer.removeBreakpoint(id).foreach(replyTo ! EditorResponse(_))

        // ===== Internal =====
        case PullNext(replyTo)              => sequencer.pullNext().foreach(replyTo.tell)
        case MaybeNext(replyTo)             => sequencer.mayBeNext.foreach(replyTo.tell)
        case ReadyToExecuteNext(replyTo)    => sequencer.readyToExecuteNext().foreach(replyTo.tell)
        case UpdateFailure(failureResponse) => sequencer.updateFailure(failureResponse)
      }
      Behaviors.same
    }
}
