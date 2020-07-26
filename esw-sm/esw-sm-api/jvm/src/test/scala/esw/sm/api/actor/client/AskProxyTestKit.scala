package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}

abstract class AskProxyTestKit[Msg, Impl](implicit actorSystem: ActorSystem[_]) {
  protected def make(actorRef: ActorRef[Msg]): Impl

  def withBehavior(pf: PartialFunction[Msg, Unit]): Assertable = {
    val behavior = Behaviors.receiveMessagePartial[Msg] { req =>
      pf(req)
      Behaviors.stopped
    }
    val stubActorRef = actorSystem.systemActorOf(behavior, "ask-test-kit-stub")
    val proxy        = make(stubActorRef)
    assertion => assertion(proxy)
  }

  trait Assertable {
    def check(assertion: Impl => Unit): Unit
  }
}
