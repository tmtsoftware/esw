package esw.gateway.impl

import akka.stream.scaladsl.Source

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationLong

object Utils {
  def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis

  def emptySourceWithError[S, E](error: E): Source[S, Future[Some[E]]] =
    Source.empty.mapMaterializedValue(_ => Future.successful(Some(error)))

  def sourceWithNoError[O, M](source: Source[O, M]): Source[O, Future[None.type]] = {
    source.mapMaterializedValue(_ => Future.successful(None))
  }
}
