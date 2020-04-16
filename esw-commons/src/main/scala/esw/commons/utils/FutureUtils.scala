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
        // complete promise as soon as one of the future completes and predicate matches
        if (predicate(value)) p.trySuccess(Some(value))
        None
      }
    }

    // At this stage, one of the following thing will happen:
    // 1. Promise is already completed with value and tryComplete will return false
    // 2. All the futures are completed but predicate did not match then promise completes with None
    // 3. One or more futures failed and predicate did not match for any of the successful future then promise completes with failure
    result.map(_.flatten.headOption).onComplete(p.tryComplete)
    p.future
  }

}
