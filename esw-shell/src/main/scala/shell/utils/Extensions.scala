package shell.utils

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}

object Extensions {
  implicit class FutureExt[T](val future: Future[T]) extends AnyVal {
    def await(duration: FiniteDuration = Timeouts.defaultDuration): T = Await.result(future, duration)
    def get: T                                                        = future.await()
  }
}
