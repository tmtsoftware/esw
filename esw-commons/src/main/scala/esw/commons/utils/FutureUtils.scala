package esw.commons.utils

import scala.concurrent.{ExecutionContext, Future, Promise}

object FutureUtils {
  final def firstCompletedOf[T](
      futures: IterableOnce[Future[T]]
  )(predicate: T => Boolean)(implicit executor: ExecutionContext): Future[Option[T]] = {
    val list = futures.iterator.toList
    val p    = Promise[Option[T]]()
    val result = Future.traverse(list) { future =>
      future.map { value =>
        if (predicate(value)) p.trySuccess(Some(value))
        None
      }
    }

    result.map(_.flatten.headOption).onComplete(p.tryComplete)
    p.future
  }

}
