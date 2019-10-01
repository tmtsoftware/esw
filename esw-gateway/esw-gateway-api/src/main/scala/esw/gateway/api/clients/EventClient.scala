package esw.gateway.api.clients

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.EventApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest.{GetEvent, PublishEvent}
import esw.gateway.api.protocol.WebsocketRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.api.protocol._
import msocket.api.Transport
import msocket.api.models.StreamStatus

import scala.concurrent.Future

class EventClient(postClient: Transport[PostRequest], websocketClient: Transport[WebsocketRequest])
    extends EventApi
    with GatewayCodecs {

  override def publish(event: Event): Future[Either[EventServerUnavailable.type, Done]] =
    postClient.requestResponse[Either[EventServerUnavailable.type, Done]](PublishEvent(event))

  override def get(eventKeys: Set[EventKey]): Future[Either[GetEventError, Set[Event]]] =
    postClient.requestResponse[Either[GetEventError, Set[Event]]](GetEvent(eventKeys))

  override def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[StreamStatus]] = {
    websocketClient.requestStreamWithStatus[Event](Subscribe(eventKeys, maxFrequency))
  }

  override def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String = "*"
  ): Source[Event, Future[StreamStatus]] = {
    websocketClient.requestStreamWithStatus[Event](
      SubscribeWithPattern(subsystem, maxFrequency, pattern)
    )
  }
}
