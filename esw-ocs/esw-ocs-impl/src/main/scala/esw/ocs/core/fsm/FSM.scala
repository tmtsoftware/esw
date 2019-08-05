package esw.ocs.core.fsm

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import esw.ocs.api.models.messages.FSM.{SequencerMessage, Unhandled}

import scala.reflect.ClassTag

trait FSM {
  protected def receive[B <: SequencerMessage: ClassTag](
      stateName: String
  )(f: B => Behavior[SequencerMessage]): Behavior[SequencerMessage] =
    Behaviors.receiveMessage {
      case m: B => f(m)
      case m =>
        m.replyTo ! Unhandled(stateName, m.getClass.getSimpleName)
        Behaviors.same
    }
}
