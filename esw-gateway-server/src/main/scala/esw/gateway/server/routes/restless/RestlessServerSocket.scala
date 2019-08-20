package esw.gateway.server.routes.restless

import akka.NotUsed
import akka.stream.scaladsl.Source
import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.messages.WebSocketMessage
import esw.gateway.server.routes.restless.messages.WebSocketMessage.{
  CurrentStateSubscriptionCommandMessage,
  PatternSubscribeEventMessage,
  QueryCommandMessage,
  SubscribeEventMessage
}
import msocket.core.api.Payload
import msocket.core.api.ToPayload.{FutureToPayload, SourceWithErrorToPayload}
import msocket.core.server.ServerSocket

class RestlessServerSocket(gatewayApi: GatewayApi) extends ServerSocket[WebSocketMessage] with RestlessCodecs {

  import gatewayApi.cswCtx.actorRuntime.{ec, mat}

  override def requestStream(request: WebSocketMessage): Source[Payload[_], NotUsed] = request match {
    case QueryCommandMessage(componentType, componentName, runId) =>
      gatewayApi.queryFinal(componentType, componentName, runId).payload
    case CurrentStateSubscriptionCommandMessage(componentType, componentName, stateNames, maxFrequency) =>
      gatewayApi.subscribeCurrentState(componentType, componentName, stateNames, maxFrequency).resultPayloads
    case SubscribeEventMessage(eventKeys, maxFrequency) => gatewayApi.subscribe(eventKeys, maxFrequency).resultPayloads
    case PatternSubscribeEventMessage(subsystem, maxFrequency, pattern) =>
      gatewayApi.pSubscribe(subsystem, maxFrequency, pattern).resultPayloads
  }
}
