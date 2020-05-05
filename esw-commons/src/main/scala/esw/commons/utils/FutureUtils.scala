package esw.commons.utils

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, Supervision}
import esw.commons.Timeouts

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object FutureUtils {

  final def firstCompletedOf[T](
      futures: List[Future[T]]
  )(predicate: T => Boolean)(implicit actorSystem: ActorSystem[_]): Future[Option[T]] =
    Source(futures)
      .mapAsyncUnordered(futures.size)(identity)
      .withAttributes(ActorAttributes.supervisionStrategy { case NonFatal(_) => Supervision.Resume })
      .filter(predicate)
      .runWith(Sink.headOption)

  implicit final class FutureUtil[T](private val f: Future[T]) extends AnyVal {
    def block: T = Await.result(f, Timeouts.DefaultTimeout)
  }
}
