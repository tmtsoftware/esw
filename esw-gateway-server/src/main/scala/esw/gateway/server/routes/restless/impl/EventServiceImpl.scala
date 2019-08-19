package esw.gateway.server.routes.restless.impl

import akka.Done
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber, EventSubscription}
import csw.params.events.{Event, EventKey}
import esw.gateway.server.routes.restless.api.EventServiceApi
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg.{EmptyEventKeys, InvalidMaxFrequency}
import esw.gateway.server.routes.restless.messages.HttpRequestMsg.{GetEventMsg, PublishEventMsg}
import esw.gateway.server.routes.restless.messages.WebSocketMsg.SubscribeEventMsg
import esw.gateway.server.routes.restless.messages.{ErrorResponseMsg, WebSocketMsg}
import esw.gateway.server.routes.restless.utils.Utils.emptySourceWithError
import esw.http.core.commons.Utils
import esw.http.core.commons.Utils.maxFrequencyToDuration
import esw.http.core.utils.CswContext

import scala.concurrent.Future

class EventServiceImpl(cswCtx: CswContext) extends EventServiceApi {

  import cswCtx._
  import actorRuntime.typedSystem.executionContext

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  override def publish(publishEventMsg: PublishEventMsg): Future[Done] = publisher.publish(publishEventMsg.event)

  override def get(getEventMsg: GetEventMsg): Future[Either[ErrorResponseMsg, Set[Event]]] = {
    import getEventMsg._
    if (eventKeys.nonEmpty) subscriber.get(eventKeys.toEventKeys).map(Right(_))
    else Future.successful(Left(EmptyEventKeys()))
  }

  def subscribe(subscribeEventMsg: SubscribeEventMsg): Source[Event, Future[Option[ErrorResponseMsg]]] = {
    import subscribeEventMsg._

    if (eventKeys.nonEmpty) {
      maxFrequency match {
        case Some(x) if x <= 0 => emptySourceWithError(InvalidMaxFrequency())
        case Some(frequency) =>
          subscriber
            .subscribe(eventKeys.toEventKeys, Utils.maxFrequencyToDuration(frequency), RateLimiterMode)
            .mapMaterializedValue(_ => Future.successful(None))
        case None => subscriber.subscribe(eventKeys.toEventKeys).mapMaterializedValue(_ => Future.successful(None))
      }
    } else emptySourceWithError(EmptyEventKeys())
  }

  def pSubscribe(
      patternSubscribeEventMsg: WebSocketMsg.PatternSubscribeEventMsg
  ): Source[Event, Future[Option[ErrorResponseMsg]]] = {
    import patternSubscribeEventMsg._

    def events: Source[Event, EventSubscription] = subscriber.pSubscribe(subsystem, pattern)
    maxFrequency match {
      case Some(x) if x <= 0 => emptySourceWithError(InvalidMaxFrequency())
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
