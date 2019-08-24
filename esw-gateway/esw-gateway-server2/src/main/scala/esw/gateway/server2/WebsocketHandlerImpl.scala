package esw.gateway.server2

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.WebsocketRequest
import esw.gateway.api.messages.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.{CommandServiceApi, EventServiceApi}
import mscoket.impl.ToPayload._
import msocket.api.{Payload, WebsocketHandler}

import scala.concurrent.ExecutionContext

class WebsocketHandlerImpl(commandServiceApi: CommandServiceApi, eventServiceApi: EventServiceApi)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends WebsocketHandler[WebsocketRequest]
    with RestlessCodecs {

  override def handle(request: WebsocketRequest): Source[Payload[_], NotUsed] = request match {
    case QueryFinal(componentId, runId) =>
      commandServiceApi.queryFinal(componentId, runId).payload
    case SubscribeCurrentState(componentId, stateNames, maxFrequency) =>
      commandServiceApi.subscribeCurrentState(componentId, stateNames, maxFrequency).resultPayloads

    case Subscribe(eventKeys, maxFrequency) => eventServiceApi.subscribe(eventKeys, maxFrequency).resultPayloads
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      eventServiceApi.pSubscribe(subsystem, maxFrequency, pattern).resultPayloads
  }
}
