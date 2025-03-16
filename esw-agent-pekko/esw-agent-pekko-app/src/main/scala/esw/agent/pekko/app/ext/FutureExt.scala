package esw.agent.pekko.app.ext

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter.TypedActorSystemOps
import org.apache.pekko.pattern

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}

/**
 * This is a convenience utility on top of [[scala.concurrent.Future]].
 */
object FutureExt {
  private lazy val timedOutFuture = Future.failed(new TimeoutException)

  implicit class FutureOps[T](private val future: Future[T]) extends AnyVal {
    def timeout(duration: FiniteDuration)(implicit system: ActorSystem[?]): Future[T] = {
      import system.executionContext
      val delayedTimeout = pattern.after(duration, system.toClassic.scheduler)(timedOutFuture)
      Future.firstCompletedOf(List(future, delayedTimeout))
    }
  }

}
