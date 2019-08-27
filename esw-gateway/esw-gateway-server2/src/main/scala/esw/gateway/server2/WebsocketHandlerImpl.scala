package esw.gateway.server2

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.WebsocketRequest
import esw.gateway.api.messages.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.{CommandApi, EventApi}
import mscoket.impl.ToPayload._
import msocket.api.RequestHandler

import scala.concurrent.ExecutionContext

class WebsocketHandlerImpl(commandApi: CommandApi, eventApi: EventApi)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends RequestHandler[WebsocketRequest, Source[Message, NotUsed]]
    with RestlessCodecs {

  override def handle(request: WebsocketRequest): Source[Message, NotUsed] = request match {
    case QueryFinal(componentId, runId) => commandApi.queryFinal(componentId, runId).payload
    case SubscribeCurrentState(componentId, stateNames, maxFrequency) =>
      commandApi.subscribeCurrentState(componentId, stateNames, maxFrequency).resultPayloads
    case Subscribe(eventKeys, maxFrequency) => eventApi.subscribe(eventKeys, maxFrequency).resultPayloads
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      eventApi.pSubscribe(subsystem, maxFrequency, pattern).resultPayloads
  }
}
