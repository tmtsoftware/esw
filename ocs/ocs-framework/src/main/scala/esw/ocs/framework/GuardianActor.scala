package esw.ocs.framework

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Props}

object GuardianActor {
  sealed trait GuardianMsg
  final case class Spawn[T](behavior: Behavior[T], name: String, props: Props)(val replyTo: ActorRef[ActorRef[T]])
      extends GuardianMsg
  final case class ShutdownChildren(replyTo: ActorRef[Done])                 extends GuardianMsg
  final private[framework] case class ShutdownReply(replyTo: ActorRef[Done]) extends GuardianMsg

  val behavior: Behavior[GuardianMsg] = Behaviors.setup[GuardianMsg] { ctx =>
    Behaviors.receiveMessage[GuardianMsg] {
      case msg @ Spawn(beh, name, props) =>
        val actorRef = ctx.spawn(beh, name, props)
        msg.replyTo ! actorRef
        Behaviors.same

      case msg @ ShutdownChildren(replyTo) =>
        if (ctx.children.isEmpty) {
          msg.replyTo ! Done
        } else {
          ctx.children.foreach { child =>
            ctx.watchWith(child, ShutdownReply(replyTo))
            ctx.stop(child)
          }
        }
        Behaviors.same

      case ShutdownReply(replyTo) =>
        if (ctx.children.isEmpty) {
          replyTo ! Done
        }
        Behaviors.same
    }
  }
}
