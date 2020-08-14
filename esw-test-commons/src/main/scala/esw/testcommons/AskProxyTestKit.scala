package esw.testcommons

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}

abstract class AskProxyTestKit[Msg, Impl](implicit actorSystem: ActorSystem[_]) {
  protected def make(actorRef: ActorRef[Msg]): Impl

  def withBehavior(pf: PartialFunction[Msg, Unit]): Assertable = {
    var requestReceived = false
    val behavior = Behaviors.receiveMessagePartial[Msg] { req =>
      requestReceived = true
      if (pf.isDefinedAt(req)) pf(req) else senderOf(req) ! s"Unhandled message: $req"
      Behaviors.stopped
    }
    val stubActorRef = actorSystem.systemActorOf(behavior, s"ask-test-kit-stub-${uuid()}")
    val proxy        = make(stubActorRef)
    assertion => assertion(proxy); assert(requestReceived, s"mocked request was not received")
  }

  private def senderOf(req: Msg) = req.asInstanceOf[{ def replyTo: ActorRef[Any] }].replyTo
  private def uuid()             = java.util.UUID.randomUUID.toString

  trait Assertable {
    def check(assertion: Impl => Unit): Unit
  }
}
