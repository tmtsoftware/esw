package esw.gateway.impl

import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Source}
import msocket.api.models.{StreamError, StreamStarted}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationLong

object Utils {
  def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis

  def emptySourceWithError[S](error: StreamError): Source[S, Future[StreamError]] =
    Source.empty.mapMaterializedValue(_ => Future.successful(error))

  def sourceWithNoError[O, M](source: Source[O, M]): Source[O, Future[StreamStarted]] = {
    source
      .viaMat(KillSwitches.single)(Keep.right)
      .mapMaterializedValue(switch => Future.successful(StreamStarted(() => switch.shutdown())))
  }
}
