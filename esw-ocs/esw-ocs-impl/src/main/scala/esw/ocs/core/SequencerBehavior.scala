package esw.ocs.core

import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.{ProcessSequence, SequencerMsg}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.error.{AbortError, GoOfflineError, GoOnlineError, ShutdownError}
import esw.ocs.api.models.messages.{EditorResponse, LifecycleResponse, StepListResponse}
import esw.ocs.dsl.ScriptDsl
import esw.ocs.utils.FutureEitherExt._

object SequencerBehavior {
  def behavior(sequencer: Sequencer, script: ScriptDsl): Behaviors.Receive[SequencerMsg] =
    Behaviors.receive[SequencerMsg] { (ctx, msg) =>
      import ctx.executionContext
      msg match {
        // ===== External Lifecycle =====
        case GoOnline(replyTo) =>
          script.executeGoOnline().toEither(ex => GoOnlineError(ex.getMessage)).foreach(replyTo ! LifecycleResponse(_))

        case GoOffline(replyTo) =>
          script.executeGoOffline().toEither(ex => GoOfflineError(ex.getMessage)).foreach(replyTo ! LifecycleResponse(_))

        case Shutdown(replyTo) =>
          script.executeShutdown().toEither(ex => ShutdownError(ex.getMessage)).foreach(replyTo ! LifecycleResponse(_))

        case Abort(replyTo) =>
          script.executeAbort().toEither(ex => AbortError(ex.getMessage)).foreach(replyTo ! LifecycleResponse(_))

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
