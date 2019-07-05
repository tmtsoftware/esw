package esw.gateway.server.routes

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.{Directive0, Route}
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber}
import csw.params.core.formats.JsonSupport
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import esw.template.http.server.commons.RichSourceExt.RichSource
import esw.template.http.server.csw.utils.CswContext

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.language.postfixOps

class EventRoutes(cswCtx: CswContext) extends JsonSupport with PlayJsonSupport {
  import cswCtx._

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  val route: Route = {

    pathPrefix("event") {
      pathEnd {
        post {
          entity(as[Event]) { event =>
            complete(publisher.publish(event))
          }
        } ~
        get {
          parameter("key".as[String].*) { keys =>
            validateKeys(keys) {
              val eventualEvents = subscriber.get(keys.toEventKeys)
              complete(eventualEvents)
            }
          }
        }
      } ~
      pathPrefix("subscribe") {
        get {
          pathEnd {
            parameters(("key".as[String].*, "max-frequency".as[Int])) { (keys, maxFrequency) =>
              validateKeys(keys) {
                validateFrequency(maxFrequency) {
                  complete(
                    subscriber
                      .subscribe(keys.toEventKeys, maxFrequencyToDuration(maxFrequency), RateLimiterMode)
                      .toSSE
                  )
                }
              }
            }
          } ~
          path(Segment) { subsystem =>
            val sub = Subsystem.withNameInsensitive(subsystem)
            parameters(("max-frequency".as[Int], "pattern" ?)) { (maxFrequency, pattern) =>
              validateFrequency(maxFrequency) {
                val events = pattern match {
                  case Some(p) => subscriber.pSubscribe(sub, p)
                  case None    => subscriber.pSubscribe(sub, "*")
                }

                complete(
                  events
                    .via(eventSubscriberUtil.subscriptionModeStage(maxFrequencyToDuration(maxFrequency), RateLimiterMode))
                    .toSSE
                )
              }
            }
          }
        }
      }
    }
  }

  private def validateKeys(keys: Iterable[String]): Directive0 = {
    validate(keys.nonEmpty, "Request is missing query parameter key")
  }

  private def validateFrequency(maxFrequency: Int): Directive0 = {
    validate(maxFrequency > 0, "Max frequency should be greater than zero")
  }

  private def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis

  implicit class RichEventKeys(keys: Iterable[String]) {
    def toEventKeys: Set[EventKey] = keys.map(EventKey(_)).toSet
  }
}
