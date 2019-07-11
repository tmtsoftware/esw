package esw.ocs.framework.core

import akka.actor.typed.scaladsl.Behaviors
import esw.ocs.framework.api.models.messages.SequencerMsg
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.dsl.ScriptDsl

object SequencerBehavior {
  def behavior(sequencer: Sequencer, script: ScriptDsl): Behaviors.Receive[SequencerMsg] =
    Behaviors.receive[SequencerMsg] { (ctx, msg) =>
      import ctx.executionContext

      msg match {
        // ===== External Lifecycle =====
        case Shutdown(replyTo) ⇒ script.executeShutdown().onComplete(replyTo.tell)
        case Abort(replyTo)    ⇒ script.executeAbort().onComplete(replyTo.tell)

        // ===== External Editor =====
        case ProcessSequence(sequence, replyTo) ⇒ sequencer.processSequence(sequence).foreach(replyTo.tell)
        case Available(replyTo)                 ⇒ sequencer.isAvailable.foreach(replyTo.tell)
        case GetSequence(replyTo)               ⇒ sequencer.getSequence.foreach(replyTo.tell)
        case GetPreviousSequence(replyTo)       ⇒ sequencer.getPreviousSequence.foreach(replyTo.tell)
        case Add(commands, replyTo)             ⇒ sequencer.add(commands).foreach(replyTo.tell)
        case Pause(replyTo)                     ⇒ sequencer.pause.foreach(replyTo.tell)
        case Resume(replyTo)                    ⇒ sequencer.resume.foreach(replyTo.tell)
        case Reset(replyTo)                     ⇒ sequencer.reset().foreach(replyTo.tell)
        case Replace(id, commands, replyTo)     ⇒ sequencer.replace(id, commands).foreach(replyTo.tell)
        case Prepend(commands, replyTo)         ⇒ sequencer.prepend(commands).foreach(replyTo.tell)
        case Delete(id, replyTo)                ⇒ sequencer.delete(id).foreach(replyTo.tell)
        case InsertAfter(id, commands, replyTo) ⇒ sequencer.insertAfter(id, commands).foreach(replyTo.tell)
        case AddBreakpoint(id, replyTo)         ⇒ sequencer.addBreakpoint(id).foreach(replyTo.tell)
        case RemoveBreakpoint(id, replyTo)      ⇒ sequencer.removeBreakpoint(id).foreach(replyTo.tell)

        // ===== Internal =====
        case PullNext(replyTo)              ⇒ sequencer.pullNext().foreach(replyTo.tell)
        case MaybeNext(replyTo)             ⇒ sequencer.mayBeNext.foreach(replyTo.tell)
        case ReadyToExecuteNext(replyTo)    ⇒ sequencer.readyToExecuteNext().foreach(replyTo.tell)
        case UpdateFailure(failureResponse) => sequencer.updateFailure(failureResponse)
      }
      Behaviors.same
    }
}
