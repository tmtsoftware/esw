package esw.http.core.commons

import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import io.bullet.borer.{Encoder, Json}

import scala.concurrent.duration.DurationDouble

object RichSourceExt {
  implicit class RichSource[A: Encoder, B](source: Source[A, B]) {
    def toSSE: Source[ServerSentEvent, B] =
      source
        .map(r => ServerSentEvent(Json.encode(r).toUtf8String))
        .keepAlive(30.seconds, () => ServerSentEvent.heartbeat)
  }
}
