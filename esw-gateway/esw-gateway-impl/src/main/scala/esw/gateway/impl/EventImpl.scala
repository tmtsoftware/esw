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
import msocket.api.models.StreamStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import SourceExtensions.RichSource

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
      } else Future.successful(Left(EmptyEventKeys))
  }

  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[StreamStatus]] = {

    if (eventKeys.nonEmpty) {
      maxFrequency match {
        case Some(x) if x <= 0 =>
          Source.empty.withError(InvalidMaxFrequency.toStreamError)
        case Some(frequency) =>
          subscriber.subscribe(eventKeys, Utils.maxFrequencyToDuration(frequency), RateLimiterMode).withSubscription()
        case None => subscriber.subscribe(eventKeys).withSubscription()
      }
    } else Source.empty.withError(EmptyEventKeys.toStreamError)
  }

  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String = "*"
  ): Source[Event, Future[StreamStatus]] = {

    def events: Source[Event, EventSubscription] = subscriber.pSubscribe(subsystem, pattern)
    maxFrequency match {
      case Some(x) if x <= 0 =>
        Source.empty.withError(InvalidMaxFrequency.toStreamError)
      case Some(f) =>
        events.via(eventSubscriberUtil.subscriptionModeStage(Utils.maxFrequencyToDuration(f), RateLimiterMode)).withSubscription()
      case None =>
        events.withSubscription()
    }
  }

}
