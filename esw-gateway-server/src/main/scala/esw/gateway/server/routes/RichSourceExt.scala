package esw.gateway.server.routes

import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration.FiniteDuration

object RichSourceExt {
  implicit class RichSource[A: Format, B](source: Source[A, B]) {
    def toSSE(heartbeatDuration: FiniteDuration): Source[ServerSentEvent, B] =
      source
        .map(r => ServerSentEvent(Json.stringify(Json.toJson(r))))
        .keepAlive(heartbeatDuration, () => ServerSentEvent.heartbeat)
  }

}
