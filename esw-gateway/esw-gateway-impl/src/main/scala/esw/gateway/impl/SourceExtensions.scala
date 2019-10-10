package esw.gateway.impl

import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Source}
import msocket.api.models.{StreamError, StreamStarted}

import scala.concurrent.Future

object SourceExtensions {

  implicit class RichSource[T, Mat](stream: Source[T, Mat]) {
    def withError(error: StreamError): Source[T, Future[StreamError]] =
      stream
        .mapMaterializedValue(_ => Future.successful(error))

    def withSubscription(): Source[T, Future[StreamStarted]] =
      stream
        .viaMat(KillSwitches.single)(Keep.right)
        .mapMaterializedValue(switch => Future.successful(StreamStarted(() => switch.shutdown())))
  }
}
