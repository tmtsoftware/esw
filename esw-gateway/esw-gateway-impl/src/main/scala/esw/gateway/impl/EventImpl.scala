package esw.gateway.impl

import akka.Done
import akka.stream.scaladsl.Source
import csw.event.api.exceptions.{EventServerNotAvailable, PublishFailure}
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber, EventSubscription}
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import esw.gateway.api.EventApi
import esw.gateway.api.protocol._
import msocket.jvm.SourceExtension.WithSubscription
import msocket.api.Subscription

import scala.concurrent.{ExecutionContext, Future}

class EventImpl(eventService: EventService, eventSubscriberUtil: EventSubscriberUtil)(implicit ec: ExecutionContext)
    extends EventApi {

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  override def publish(event: Event): Future[Done] =
    publisher.publish(event).recover { case PublishFailure(_, _) => throw new EventServerUnavailable }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    if (eventKeys.nonEmpty)
      subscriber.get(eventKeys).recover { case EventServerNotAvailable(_) => throw new EventServerUnavailable }
    else Future.failed(new EmptyEventKeys)
  }

  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Subscription] = {
    val stream =
      if (eventKeys.nonEmpty)
        maxFrequency match {
          case Some(x) if x <= 0 => Source.failed(InvalidMaxFrequency())
          case Some(frequency)   => subscriber.subscribe(eventKeys, Utils.maxFrequencyToDuration(frequency), RateLimiterMode)
          case None              => subscriber.subscribe(eventKeys).withSubscription()
        }
      else Source.failed(new EmptyEventKeys)

    stream.withSubscription()
  }

  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String
  ): Source[Event, Subscription] = {

    def events: Source[Event, EventSubscription] = subscriber.pSubscribe(subsystem, pattern)
    val stream = maxFrequency match {
      case Some(x) if x <= 0 => Source.failed(InvalidMaxFrequency())
      case Some(f)           => events.via(eventSubscriberUtil.subscriptionModeStage(Utils.maxFrequencyToDuration(f), RateLimiterMode))
      case None              => events
    }

    stream.withSubscription()
  }

}
