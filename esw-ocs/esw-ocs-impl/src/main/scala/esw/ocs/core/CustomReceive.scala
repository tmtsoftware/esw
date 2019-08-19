package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import esw.ocs.api.models.SequencerBehaviorState
import esw.ocs.api.models.messages.SequencerMessages.{EswSequencerMessage, LoadAndStartSequenceInternal}
import esw.ocs.api.models.messages.{SequenceResponse, Unhandled}
import esw.ocs.internal.Timeouts

import scala.concurrent.Future
import scala.reflect.ClassTag

private[ocs] trait CustomReceive {

  protected def receive[T <: SequencerMsg: ClassTag](stateName: SequencerBehaviorState)(
      f: (ActorContext[SequencerMsg], T) => Behavior[SequencerMsg]
  ): Behavior[SequencerMsg] = Behaviors.receive { (ctx, msg) =>
    import ctx.executionContext
    implicit val timeout: Timeout     = Timeouts.LongTimeout
    implicit val scheduler: Scheduler = ctx.system.scheduler

    msg match {
      case msg: T                   => f(ctx, msg)
      case msg: EswSequencerMessage => msg.replyTo ! Unhandled(stateName, msg.getClass.getSimpleName); Behaviors.same
      case LoadAndStartSequence(sequence, replyTo) =>
        val sequenceResponseF: Future[SequenceResponse] = ctx.self ? (LoadAndStartSequenceInternal(sequence, _))
        sequenceResponseF.foreach(res => replyTo ! res.toSubmitResponse(sequence.runId))
        Behaviors.same
      case _ => Behaviors.unhandled
    }
  }

}
