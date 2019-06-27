package esw.gateway.server.routes

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber, EventSubscription}
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.server.JsonSupportExt
import esw.template.http.server.CswContext
import play.api.libs.json.Json

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.language.postfixOps

class EventRoutes(cswCtx: CswContext) extends JsonSupportExt {
  import cswCtx._
  import actorRuntime.mat

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher
  private val eventSubscriberUtil      = new EventSubscriberUtil

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
              complete(
                subscriber
                  .subscribe(keys.toEventKeys, frequncyToTime(frequency), RateLimiterMode)
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
                  .via(eventSubscriberUtil.subscriptionModeStage(frequncyToTime(frequency), RateLimiterMode))
                  .toSSE
              )
            }
          }
        }
      }
    }
  }

  private def frequncyToTime(frequency: Int): FiniteDuration = (1000 / frequency).millis

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
