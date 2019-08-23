package esw.highlevel.dsl

import akka.Done
import akka.actor.Cancellable
import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber, EventSubscription}
import csw.params.events.{Event, EventKey}
import csw.time.core.models.{TMTTime, UTCTime}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class EventServiceDsl(eventService: EventService) {

  private lazy val publisher: EventPublisher   = eventService.defaultPublisher
  private lazy val subscriber: EventSubscriber = eventService.defaultSubscriber

  def publish(event: Event): Future[Done] = publisher.publish(event)

  def publish(
      every: FiniteDuration,
      startTime: TMTTime = UTCTime.now()
  )(
      eventGenerator: => Option[Event],
      onError: PublishFailure => Unit = _ => ()
  )(implicit ec: ExecutionContext): Cancellable = publisher.publishAsync(Future(eventGenerator) _, startTime, every, onError)

  def subscribe(eventKeys: EventKey*)(callback: Event => Unit)(implicit ec: ExecutionContext): EventSubscription =
    subscriber.subscribeAsync(eventKeys.toSet, event => Future(callback(event)))

  def get(eventKeys: EventKey*): Future[Set[Event]] = subscriber.get(eventKeys.toSet)
}
