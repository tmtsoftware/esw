package esw.gateway.api.clients

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.EventServiceApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayHttpRequest.{GetEvent, PublishEvent}
import esw.gateway.api.messages.GatewayWebsocketRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.api.messages.{EmptyEventKeys, EventError, GatewayWebsocketRequest, InvalidMaxFrequency}
import msocket.api.{ClientSocket, HttpClient}

import scala.concurrent.Future

class EventClient(httpClient: HttpClient, socket: ClientSocket[GatewayWebsocketRequest])
    extends EventServiceApi
    with RestlessCodecs {

  override def publish(event: Event): Future[Done] = {
    httpClient.post[PublishEvent, Done](PublishEvent(event))
  }

  override def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]] = {
    httpClient.post[GetEvent, Either[EmptyEventKeys, Set[Event]]](GetEvent(eventKeys))
  }

  override def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[Option[EventError]]] = {
    socket.requestStreamWithError[Event, EventError](Subscribe(eventKeys, maxFrequency))
  }

  override def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String
  ): Source[Event, Future[Option[InvalidMaxFrequency]]] = {
    socket.requestStreamWithError[Event, InvalidMaxFrequency](SubscribeWithPattern(subsystem, maxFrequency, pattern))
  }
}
