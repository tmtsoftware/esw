package esw.gateway.server.routes.restless.impl

import akka.Done
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber, EventSubscription}
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.messages.{EmptyEventKeys, EventErrorMessage, InvalidMaxFrequency}
import esw.gateway.server.routes.restless.utils.SourceExtension
import esw.http.core.commons.Utils
import esw.http.core.commons.Utils.maxFrequencyToDuration

import scala.concurrent.Future

trait EventGatewayImpl extends GatewayApi {

  import cswCtx._
  import actorRuntime.ec

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  override def publish(event: Event): Future[Done] = publisher.publish(event)

  override def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]] = {
    if (eventKeys.nonEmpty) subscriber.get(eventKeys).map(Right(_))
    else Future.successful(Left(EmptyEventKeys()))
  }

  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[Option[EventErrorMessage]]] = {

    if (eventKeys.nonEmpty) {
      maxFrequency match {
        case Some(x) if x <= 0 => SourceExtension.emptyWithError(InvalidMaxFrequency())
        case Some(frequency) =>
          subscriber
            .subscribe(eventKeys, Utils.maxFrequencyToDuration(frequency), RateLimiterMode)
            .mapMaterializedValue(_ => Future.successful(None))
        case None => subscriber.subscribe(eventKeys).mapMaterializedValue(_ => Future.successful(None))
      }
    } else SourceExtension.emptyWithError(EmptyEventKeys())
  }

  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String = "*"
  ): Source[Event, Future[Option[InvalidMaxFrequency]]] = {

    def events: Source[Event, EventSubscription] = subscriber.pSubscribe(subsystem, pattern)
    maxFrequency match {
      case Some(x) if x <= 0 => SourceExtension.emptyWithError(InvalidMaxFrequency())
      case Some(f) =>
        events
          .via(eventSubscriberUtil.subscriptionModeStage(maxFrequencyToDuration(f), RateLimiterMode))
          .mapMaterializedValue(_ => Future.successful(None))
      case None => events.mapMaterializedValue(_ => Future.successful(None))
    }

  }

  implicit class RichEventKeys(keys: Iterable[String]) {
    def toEventKeys: Set[EventKey] = keys.map(EventKey(_)).toSet
  }

}
