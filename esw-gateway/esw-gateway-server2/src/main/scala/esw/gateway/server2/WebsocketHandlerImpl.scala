package esw.gateway.server2

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.WebsocketRequest
import esw.gateway.api.messages.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.{CommandApi, EventApi}
import mscoket.impl.ToPayload._
import msocket.api.{Payload, WebsocketHandler}

import scala.concurrent.ExecutionContext

class WebsocketHandlerImpl(commandApi: CommandApi, eventApi: EventApi)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends WebsocketHandler[WebsocketRequest]
    with RestlessCodecs {

  override def handle(request: WebsocketRequest): Source[Payload[_], NotUsed] = request match {
    case QueryFinal(componentId, runId) => commandApi.queryFinal(componentId, runId).payload
    case SubscribeCurrentState(componentId, stateNames, maxFrequency) =>
      commandApi.subscribeCurrentState(componentId, stateNames, maxFrequency).resultPayloads
    case Subscribe(eventKeys, maxFrequency) => eventApi.subscribe(eventKeys, maxFrequency).resultPayloads
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      eventApi.pSubscribe(subsystem, maxFrequency, pattern).resultPayloads
  }
}
