package esw.gateway.server.handlers

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.WebsocketRequest
import esw.gateway.api.protocol.WebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.{CommandApi, EventApi}
import msocket.impl.ws.WebsocketStreamExtensions
import msocket.api.MessageHandler
import msocket.impl.Encoding

class WebsocketHandlerImpl(commandApi: CommandApi, eventApi: EventApi, val encoding: Encoding[_])(
    implicit actorSystem: ActorSystem[_]
) extends MessageHandler[WebsocketRequest, Source[Message, NotUsed]]
    with GatewayCodecs
    with WebsocketStreamExtensions {

  override def handle(request: WebsocketRequest): Source[Message, NotUsed] = request match {
    case QueryFinal(componentId, runId) => futureAsStream(commandApi.queryFinal(componentId, runId))
    case SubscribeCurrentState(componentId, stateNames, maxFrequency) =>
      streamWithStatus(commandApi.subscribeCurrentState(componentId, stateNames, maxFrequency))
    case Subscribe(eventKeys, maxFrequency) => streamWithStatus(eventApi.subscribe(eventKeys, maxFrequency))
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      streamWithStatus(eventApi.pSubscribe(subsystem, maxFrequency, pattern))
  }
}
