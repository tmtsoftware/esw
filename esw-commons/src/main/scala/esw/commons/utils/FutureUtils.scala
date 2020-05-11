package esw.commons.utils

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, Supervision}

import scala.concurrent.Future
import scala.util.control.NonFatal

object FutureUtils {

  final def firstCompletedOf[T](
      futures: List[Future[T]]
  )(predicate: T => Boolean)(implicit actorSystem: ActorSystem[_]): Future[Option[T]] = {
    if (futures.isEmpty) Future.successful(None)
    else {
      Source(futures)
        .mapAsyncUnordered(futures.size)(identity)
        .withAttributes(ActorAttributes.supervisionStrategy {
          case NonFatal(_) => Supervision.Resume
        })
        .filter(predicate)
        .runWith(Sink.headOption)
    }
  }

  def sequential[A, B](l: Iterable[A])(fn: A => Future[B])(implicit actorSystem: ActorSystem[_]): Future[List[B]] = {
    import actorSystem.executionContext
    Source
      .fromIterator(() => l.iterator)
      .mapAsync(parallelism = 1)(fn)
      .runWith(Sink.collection)
      .map(_.toList)
  }
}
