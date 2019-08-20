package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import esw.ocs.api.models.SequencerState
import esw.ocs.api.models.messages.SequencerMessages.{EswSequencerMessage, LoadAndStartSequenceInternal}
import esw.ocs.api.models.messages.{SequenceResponse, Unhandled}
import esw.ocs.internal.Timeouts

import scala.concurrent.Future
import scala.reflect.ClassTag

private[ocs] trait CustomReceive {

  protected def receive[T <: SequencerMsg: ClassTag](state: SequencerState)(
      f: T => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = Behaviors.receive { (ctx, msg) =>
    import ctx.executionContext
    implicit val timeout: Timeout     = Timeouts.LongTimeout
    implicit val scheduler: Scheduler = ctx.system.scheduler

    msg match {
      case msg: T                   => f(msg)
      case msg: EswSequencerMessage => msg.replyTo ! Unhandled(state, msg.getClass.getSimpleName); Behaviors.same
      case LoadAndStartSequence(sequence, replyTo) =>
        val sequenceResponseF: Future[SequenceResponse] = ctx.self ? (LoadAndStartSequenceInternal(sequence, _))
        sequenceResponseF.foreach(res => replyTo ! res.toSubmitResponse(sequence.runId))
        Behaviors.same
      case _ => Behaviors.unhandled
    }
  }

}
