package esw.gateway.api.clients

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import esw.gateway.api.EventApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.*
import esw.gateway.api.protocol.GatewayRequest.{GetEvent, PublishEvent}
import esw.gateway.api.protocol.GatewayStreamRequest.{Subscribe, SubscribeObserveEvents, SubscribeWithPattern}
import msocket.api.{Subscription, Transport}

import scala.concurrent.Future

/**
 * HTTP client for the Event Service
 * @param postClient - An Transport class for HTTP calls for the Event Service
 */
class EventClient(postClient: Transport[GatewayRequest], websocketClient: Transport[GatewayStreamRequest])
    extends EventApi
    with GatewayCodecs {

  override def publish(event: Event): Future[Done]               = postClient.requestResponse[Done](PublishEvent(event))
  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = postClient.requestResponse[Set[Event]](GetEvent(eventKeys))

  override def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Subscription] =
    websocketClient.requestStream[Event](Subscribe(eventKeys, maxFrequency))

  override def pSubscribe(subsystem: Subsystem, maxFrequency: Option[Int], pattern: String): Source[Event, Subscription] =
    websocketClient.requestStream[Event](SubscribeWithPattern(subsystem, maxFrequency, pattern))

  override def subscribeObserveEvents(maxFrequency: Option[Int]): Source[Event, Subscription] =
    websocketClient.requestStream[Event](SubscribeObserveEvents(maxFrequency))
}
