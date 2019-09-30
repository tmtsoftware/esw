package esw.gateway.impl

import akka.stream.scaladsl.Source
import msocket.api.utils.{StreamError, StreamSuccess}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationLong

object Utils {
  def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis

  def emptySourceWithError[S](error: StreamError): Source[S, Future[StreamError]] =
    Source.empty.mapMaterializedValue(_ => Future.successful(error))

  def sourceWithNoError[O, M](source: Source[O, M]): Source[O, Future[StreamSuccess.type]] = {
    source.mapMaterializedValue(_ => Future.successful(StreamSuccess))
  }
}
