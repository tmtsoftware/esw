package esw.commons.extensions

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

object FutureExt {
  implicit class FutureOps[T](val future: Future[T]) extends AnyVal {
    def await(duration: FiniteDuration = 10.seconds): T = Await.result(future, duration)
    def get: T                                          = future.await()
  }
}
