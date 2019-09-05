package esw.gateway.impl

import akka.Done
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber, EventSubscription}
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.EventApi
import esw.gateway.api.messages.{EmptyEventKeys, EventError, InvalidMaxFrequency}

import scala.concurrent.{ExecutionContext, Future}

class EventImpl(eventService: EventService, eventSubscriberUtil: EventSubscriberUtil)(implicit ec: ExecutionContext)
    extends EventApi {

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  // fixme: handle failures like EventServerNotAvailable
  override def publish(event: Event): Future[Done] = publisher.publish(event)

  // fixme: handle failures like EventServerNotAvailable
  override def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]] = {
    if (eventKeys.nonEmpty) subscriber.get(eventKeys).map(Right(_))
    else Future.successful(Left(EmptyEventKeys()))
  }

  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[Option[EventError]]] = {

    if (eventKeys.nonEmpty) {
      maxFrequency match {
        case Some(x) if x <= 0 => Utils.emptySourceWithError(InvalidMaxFrequency())
        case Some(frequency) =>
          Utils.sourceWithNoError(
            subscriber
              .subscribe(eventKeys, Utils.maxFrequencyToDuration(frequency), RateLimiterMode)
          )
        case None => Utils.sourceWithNoError(subscriber.subscribe(eventKeys))
      }
    } else Utils.emptySourceWithError(EmptyEventKeys())
  }

  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String = "*"
  ): Source[Event, Future[Option[InvalidMaxFrequency]]] = {

    def events: Source[Event, EventSubscription] = subscriber.pSubscribe(subsystem, pattern)
    maxFrequency match {
      case Some(x) if x <= 0 => Utils.emptySourceWithError(InvalidMaxFrequency())
      case Some(f) =>
        Utils.sourceWithNoError(
          events
            .via(eventSubscriberUtil.subscriptionModeStage(Utils.maxFrequencyToDuration(f), RateLimiterMode))
        )
      case None => Utils.sourceWithNoError(events)
    }
  }

}
