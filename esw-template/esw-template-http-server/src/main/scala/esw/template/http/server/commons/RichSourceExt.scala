package esw.template.http.server.commons

import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration.DurationDouble

object RichSourceExt {
  implicit class RichSource[A: Format, B](source: Source[A, B]) {
    def toSSE: Source[ServerSentEvent, B] =
      source
        .map(r => ServerSentEvent(Json.stringify(Json.toJson(r))))
        .keepAlive(30.seconds, () => ServerSentEvent.heartbeat)
  }

}
