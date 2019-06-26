package esw.gateway.server.routes

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.EventSubscription
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.server.JsonSupportExt
import esw.template.http.server.CswContext
import play.api.libs.json.Json

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.language.postfixOps

class EventRoutes(cswCtx: CswContext) extends JsonSupportExt {
  import cswCtx._

  lazy val subscriber = eventService.defaultSubscriber
  lazy val publisher  = eventService.defaultPublisher

  val route: Route = {

    pathPrefix("event") {
      pathEnd {
        post {
          entity(as[Event]) { event =>
            complete(publisher.publish(event))
          }
        } ~
        get {
          parameter('keys.*) { keys =>
            val eventualEvents = subscriber.get(keys.toEventKeys)
            complete(eventualEvents)
          }
        }
      } ~
      pathPrefix("subscribe") {
        get {
          pathEnd {
            parameters(('keys.*, 'frequency.as[Int])) { (keys, frequency) =>
              val durationInMillis: FiniteDuration = (1000 / frequency).millis
              complete(
                subscriber
                  .subscribe(keys.toEventKeys, durationInMillis, RateLimiterMode)
                  .toSSE
              )
            }
          } ~
          path(Segment) { subsystem =>
            val sub = Subsystem.withNameInsensitive(subsystem)
            parameters(('frequency.as[Int], 'pattern ?)) { (frequency, pattern) =>
              val events = pattern match {
                case Some(p) => subscriber.pSubscribe(sub, p)
                case None    => subscriber.pSubscribe(sub, "*")
              }
              complete(
                events
                  .throttle(frequency, 1.seconds)
                  .toSSE
              )
            }
          }
        }
      }
    }
  }

  implicit class RichEventKeys(keys: Iterable[String]) {
    def toEventKeys: Set[EventKey] = keys.map(EventKey(_)).toSet
  }

  implicit class RichSource(source: Source[Event, EventSubscription]) {
    def toSSE: Source[ServerSentEvent, EventSubscription] =
      source
        .map(r => ServerSentEvent(Json.stringify(Json.toJson(r))))
        .keepAlive(10.seconds, () => ServerSentEvent.heartbeat)
  }
}
