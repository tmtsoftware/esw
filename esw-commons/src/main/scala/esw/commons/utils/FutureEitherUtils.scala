package esw.commons.utils
import scala.concurrent.{ExecutionContext, Future}

object FutureEitherUtils {
  implicit class FutureEither[L, R](futureEither: Future[Either[L, R]]) {
    def right[S](f: R => S)(implicit executor: ExecutionContext): Future[Either[L, S]] = futureEither.map(_.map(f))

    def left(implicit executor: ExecutionContext): Future[Either.LeftProjection[L, R]] = futureEither.map(_.left)

    def flatRight[S](f: R => Future[Either[L, S]])(implicit executor: ExecutionContext): Future[Either[L, S]] =
      futureEither.flatMap {
        case Left(l)  => Future.successful(Left(l))
        case Right(r) => f(r)
      }
  }
}
