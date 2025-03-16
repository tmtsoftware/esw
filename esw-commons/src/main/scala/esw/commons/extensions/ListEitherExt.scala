package esw.commons.extensions

/**
 * This is an extension class containing convenience functions for handling use cases involving List & Either.
 */
object ListEitherExt {
  implicit class ListEitherOps[L, R](private val eithers: List[Either[L, R]]) extends AnyVal {

    def sequence: Either[List[L], List[R]] =
      eithers.partition(_.isLeft) match {
        case (Nil, success) => Right(for (case Right(i) <- success) yield i)
        case (errs, _)      => Left(for (case Left(s) <- errs) yield s)
      }

  }
}
