package esw.gateway.api

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import msocket.api.Subscription

import scala.concurrent.Future

trait EventApi {
  /**
   * Publish a single [[csw.params.events.Event]]
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown,
   * in all other cases [[csw.event.api.exceptions.PublishFailure]] exception is thrown which wraps the underlying exception and
   * also provides the handle to the event which was failed to be published
   *
   * @param event   an event to be published
   * @return        a Future which completes when the event is published
   */
  def publish(event: Event): Future[Done]

  /**
   * Get latest events for multiple Event Keys. The latest events available for the given Event Keys will be received first.
   * If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * In case the underlying server is not available, the future fails with [[csw.event.api.exceptions.EventServerNotAvailable]] exception.
   * In all other cases of exception, the future fails with the respective exception
   *
   * @param eventKeys   a set of [[csw.params.events.EventKey]] to subscribe to
   * @return            a Future which completes with a set of latest [[csw.params.events.Event]] for the provided Event Keys
   */
  def get(eventKeys: Set[EventKey]): Future[Set[Event]]

  /**
   * Subscribe to multiple Event Keys and get a single stream of events for all event keys. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the stream is stopped after logging appropriately. In all other cases of exception, as per the default behavior, the stream will stop.
   * To avoid that, user should provide a resuming materializer while running the stream.
   *
   * @param eventKeys     a set of [[csw.params.events.EventKey]] to subscribe to
   * @param maxFrequency  the frequency with which the events are received
   * @return              a [[akka.stream.scaladsl.Source]] of [[csw.params.events.Event]]. The materialized value of the source provides
   *                      an [[msocket.api.Subscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None): Source[Event, Subscription]

  /**
   * Subscribe to events from Event Keys specified using a subsystem and a pattern to match the remaining Event Key. The latest events available for the given
   * Event Keys will be received first. If event is not published for one or more event keys, `invalid event` will be received for those Event Keys.
   *
   * At the time of invocation, in case the underlying server is not available, [[csw.event.api.exceptions.EventServerNotAvailable]] exception is thrown
   * and the subscription is stopped after logging appropriately. [[csw.event.api.scaladsl.EventSubscription!.ready]] method can be used to determine this
   * state. In all other cases of exception, the subscription resumes to receive remaining elements.
   *
   * @param subsystem     a valid `Subsystem` which represents the source of the events
   * @param maxFrequency  the frequency with which the events are received
   * @param pattern       Subscribes the client to the given patterns. Supported glob-style patterns:
   *                      - h?llo subscribes to hello, hallo and hxllo
   *                      - h*llo subscribes to hllo and heeeello
   *                      - h[ae]llo subscribes to hello and hallo, but not hillo
   *                      Use \ to escape special characters if you want to match them verbatim.
   * @return a [[akka.stream.scaladsl.Source]] of [[csw.params.events.Event]]. The materialized value of the source provides
   *         an [[msocket.api.Subscription]] which can be used to unsubscribe from all the Event Keys which were subscribed to
   */
  def pSubscribe(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*"): Source[Event, Subscription]
}
