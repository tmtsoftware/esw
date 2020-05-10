package esw.commons.extensions

object ListEitherExt {
  implicit class ListEitherOps[L, R](private val eithers: List[Either[L, R]]) extends AnyVal {

    def sequence: Either[List[L], List[R]] =
      eithers.partition(_.isLeft) match {
        case (Nil, success) => Right(for (Right(i) <- success) yield i)
        case (errs, _)      => Left(for (Left(s) <- errs) yield s)
      }

  }
}
