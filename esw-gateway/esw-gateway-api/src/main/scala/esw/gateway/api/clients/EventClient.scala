package esw.gateway.api.clients

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.EventApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.PostRequest.{GetEvent, PublishEvent}
import esw.gateway.api.messages.WebsocketRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.api.messages._
import msocket.api.RequestClient

import scala.concurrent.Future

class EventClient(postClient: RequestClient[PostRequest], websocketClient: RequestClient[WebsocketRequest])
    extends EventApi
    with RestlessCodecs {

  override def publish(event: Event): Future[Done] = {
    postClient.requestResponse[Done](PublishEvent(event))
  }

  override def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]] = {
    postClient.requestResponse[Either[EmptyEventKeys, Set[Event]]](GetEvent(eventKeys))
  }

  override def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[Option[EventError]]] = {
    websocketClient.requestStreamWithError[Event, EventError](Subscribe(eventKeys, maxFrequency))
  }

  override def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String = "*"
  ): Source[Event, Future[Option[InvalidMaxFrequency]]] = {
    websocketClient.requestStreamWithError[Event, InvalidMaxFrequency](SubscribeWithPattern(subsystem, maxFrequency, pattern))
  }
}
