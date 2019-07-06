package esw.ocs.framework.core

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.messages.{SequencerMsg, StepListError}
import esw.ocs.framework.dsl.ScriptDsl

object SequencerBehavior {
  def behavior(sequencer: Sequencer, script: ScriptDsl): Behaviors.Receive[SequencerMsg] =
    Behaviors.receive[SequencerMsg] { (ctx, msg) =>
      import ctx.executionContext

      def send[L <: StepListError, R](stepListResponse: Either[L, R], replyTo: ActorRef[Either[L, Done]]): Unit =
        replyTo ! stepListResponse.map(_ ⇒ akka.Done)

      msg match {
        // ===== External Lifecycle =====
        case Shutdown(replyTo) ⇒ script.executeShutdown().onComplete(replyTo.tell)
        case Abort(replyTo)    ⇒ script.executeAbort().onComplete(replyTo.tell)

        // ===== External Editor =====
        case ProcessSequence(sequence, replyTo) ⇒ sequencer.processSequence(sequence).foreach(replyTo.tell)
        case GetSequence(replyTo)               ⇒ sequencer.getSequence.foreach(replyTo.tell)
        case Add(commands, replyTo)             ⇒ sequencer.add(commands).foreach(send(_, replyTo))
        case Pause(replyTo)                     ⇒ sequencer.pause.foreach(send(_, replyTo))
        case Resume(replyTo)                    ⇒ sequencer.resume.foreach(send(_, replyTo))
        case DiscardPending(replyTo)            ⇒ sequencer.discardPending.foreach(send(_, replyTo))
        case Replace(id, commands, replyTo)     ⇒ sequencer.replace(id, commands).foreach(send(_, replyTo))
        case Prepend(commands, replyTo)         ⇒ sequencer.prepend(commands).foreach(send(_, replyTo))
        case Delete(id, replyTo)                ⇒ sequencer.delete(id).foreach(send(_, replyTo))
        case InsertAfter(id, commands, replyTo) ⇒ sequencer.insertAfter(id, commands).foreach(send(_, replyTo))
        case AddBreakpoint(id, replyTo)         ⇒ sequencer.addBreakpoint(id).foreach(send(_, replyTo))
        case RemoveBreakpoint(id, replyTo)      ⇒ sequencer.removeBreakpoint(id).foreach(send(_, replyTo))

        // ===== Internal =====
        case PullNext(replyTo)           ⇒ sequencer.pullNext().foreach(replyTo.tell)
        case MaybeNext(replyTo)          ⇒ sequencer.mayBeNext.foreach(replyTo.tell)
        case ReadyToExecuteNext(replyTo) ⇒ sequencer.readyToExecuteNext().foreach(replyTo.tell)
      }
      Behaviors.same
    }
}
