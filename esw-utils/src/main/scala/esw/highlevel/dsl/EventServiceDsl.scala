package esw.highlevel.dsl

import akka.Done
import akka.actor.Cancellable
import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber, EventSubscription}
import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, EventName, ObserveEvent, SystemEvent}
import csw.time.core.models.{TMTTime, UTCTime}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class EventServiceDsl(eventService: EventService) {

  private lazy val publisher: EventPublisher   = eventService.defaultPublisher
  private lazy val subscriber: EventSubscriber = eventService.defaultSubscriber

  def systemEvent(sourcePrefix: String, eventName: String, parameters: Parameter[_]*): SystemEvent =
    SystemEvent(Prefix(sourcePrefix), EventName(eventName), parameters.toSet)

  def observeEvent(sourcePrefix: String, eventName: String, parameters: Parameter[_]*): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(eventName), parameters.toSet)

  def publish(event: Event): Future[Done] = publisher.publish(event)

  def publish(
      every: FiniteDuration,
      startTime: TMTTime = UTCTime.now()
  )(
      eventGenerator: => Option[Event],
      onError: PublishFailure => Unit = _ => ()
  )(implicit ec: ExecutionContext): Cancellable = publisher.publishAsync(Future(eventGenerator), startTime, every, onError)

  private val stringToEventKey = (x: String) => EventKey(x)
  def subscribe(eventKeys: String*)(callback: Event => Unit)(implicit ec: ExecutionContext): EventSubscription =
    subscriber.subscribeAsync(eventKeys.toSet.map(stringToEventKey(_)), event => Future(callback(event)))

  def get(eventKeys: String*): Future[Set[Event]] = {
    val value: Set[EventKey] = eventKeys.toSet.map(stringToEventKey(_))
    subscriber.get(value)
  }
}
