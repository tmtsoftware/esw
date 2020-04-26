package esw.zio.commons

import zio.stream.ZStream
import zio.{IO, UIO, ZIO}

object ZIOUtils {
  final def firstCompletedOf[E, A](tasks: List[IO[E, A]])(predicate: A => Boolean): UIO[Option[A]] =
    ZStream(tasks: _*).mapMParUnordered(tasks.size)(identity).filter(predicate).runHead.catchAll(_ => ZIO.none)
}