package esw.commons.extensions

object EitherExt {
  implicit class EitherOps[L <: Throwable, R](private val either: Either[L, R]) extends AnyVal {
    def throwLeft: R = either.fold(e => throw new RuntimeException(e.getMessage), identity)
  }
}
