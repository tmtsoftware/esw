package esw.gateway.impl

import akka.Done
import akka.stream.scaladsl.Source
import csw.event.api.exceptions.{EventServerNotAvailable, PublishFailure}
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber, EventSubscription}
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.EventApi
import esw.gateway.api.protocol._
import esw.gateway.impl.SourceExtensions.RichSource
import msocket.api.models.Subscription

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EventImpl(eventService: EventService, eventSubscriberUtil: EventSubscriberUtil)(implicit ec: ExecutionContext)
    extends EventApi {

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  override def publish(event: Event): Future[Either[EventServerUnavailable.type, Done]] =
    publisher.publish(event).transform {
      case Success(_)                    => Success(Right(Done))
      case Failure(PublishFailure(_, _)) => Success(Left(EventServerUnavailable))
      case Failure(ex)                   => throw ex
    }

  override def get(eventKeys: Set[EventKey]): Future[Either[GetEventError, Set[Event]]] = {
    if (eventKeys.nonEmpty)
      subscriber.get(eventKeys).transform {
        case Success(events)                     => Success(Right(events))
        case Failure(EventServerNotAvailable(_)) => Success(Left(EventServerUnavailable))
        case Failure(ex)                         => throw ex
      }
    else Future.successful(Left(EmptyEventKeys))
  }

  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Subscription] = {
    val dd = if (eventKeys.nonEmpty) {
      maxFrequency match {
        case Some(x) if x <= 0 => Source.failed(InvalidMaxFrequency())
        case Some(frequency)   => subscriber.subscribe(eventKeys, Utils.maxFrequencyToDuration(frequency), RateLimiterMode)
        case None              => subscriber.subscribe(eventKeys).withSubscription()
      }
    }
    else Source.failed(new RuntimeException(EmptyEventKeys.msg))

    dd.withSubscription()
  }

  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String
  ): Source[Event, Subscription] = {

    def events: Source[Event, EventSubscription] = subscriber.pSubscribe(subsystem, pattern)
    val dd = maxFrequency match {
      case Some(x) if x <= 0 => Source.failed(InvalidMaxFrequency())
      case Some(f)           => events.via(eventSubscriberUtil.subscriptionModeStage(Utils.maxFrequencyToDuration(f), RateLimiterMode))
      case None              => events
    }

    dd.withSubscription()
  }

}
