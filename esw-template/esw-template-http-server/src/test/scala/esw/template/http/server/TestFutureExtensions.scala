package esw.template.http.server

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

// fixme: can we use futureValue from scalatest everywhere and remove this class?
object TestFutureExtensions {
  implicit class RichFuture[T](val f: Future[T]) extends AnyVal {
    def await: T        = Await.result(f, 20.seconds)
    def done: Future[T] = Await.ready(f, 20.seconds)
  }
}
