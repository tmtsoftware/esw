package esw.gateway.server.handlers

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.WebsocketRequest
import esw.gateway.api.protocol.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.{CommandApi, EventApi}
import mscoket.impl.ws.WebsocketStreamExtensions
import msocket.api.MessageHandler

class WebsocketHandlerImpl(commandApi: CommandApi, eventApi: EventApi)(implicit mat: Materializer)
    extends MessageHandler[WebsocketRequest, Source[Message, NotUsed]]
    with GatewayCodecs
    with WebsocketStreamExtensions {

  override def handle(request: WebsocketRequest): Source[Message, NotUsed] = request match {
    case QueryFinal(componentId, runId) => futureAsStream(commandApi.queryFinal(componentId, runId))
    case SubscribeCurrentState(componentId, stateNames, maxFrequency) =>
      streamWithError(commandApi.subscribeCurrentState(componentId, stateNames, maxFrequency))
    case Subscribe(eventKeys, maxFrequency) => streamWithError(eventApi.subscribe(eventKeys, maxFrequency))
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      streamWithError(eventApi.pSubscribe(subsystem, maxFrequency, pattern))
  }
}
