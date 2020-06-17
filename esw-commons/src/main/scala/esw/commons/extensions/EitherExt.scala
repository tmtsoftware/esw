package esw.commons.extensions

object EitherExt {
  implicit class EitherOps[L, R](private val either: Either[L, R]) extends AnyVal {
    def mapToAdt[ADT, R1 <: ADT, L1 <: ADT](rmap: R => R1, lmap: L => L1): ADT =
      either match {
        case Left(l)  => lmap(l)
        case Right(r) => rmap(r)
      }
  }
}
