package esw.agent.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.pattern

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}

object FutureExt {
  private lazy val timedOutFuture = Future.failed(new TimeoutException)

  implicit class FutureOps[T](private val future: Future[T]) extends AnyVal {
    def timeout(duration: FiniteDuration)(implicit system: ActorSystem[_]): Future[T] = {
      import system.executionContext
      val delayedTimeout = pattern.after(duration, system.toClassic.scheduler)(timedOutFuture)
      Future.firstCompletedOf(List(future, delayedTimeout))
    }
  }
}
