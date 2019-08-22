package esw.gateway.server2

import akka.NotUsed
import akka.stream.scaladsl.Source
import esw.gateway.api.GatewayApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayWebsocketRequest
import esw.gateway.api.messages.GatewayWebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import msocket.api.{Payload, ServerSocket}
import mscoket.impl.ToPayload._

class GatewayServerSocket(gatewayApi: GatewayApi) extends ServerSocket[GatewayWebsocketRequest] with RestlessCodecs {

  import gatewayApi.cswContext.actorRuntime.{ec, mat}

  override def requestStream(request: GatewayWebsocketRequest): Source[Payload[_], NotUsed] = request match {
    case QueryFinal(componentType, componentName, runId) =>
      gatewayApi.queryFinal(componentType, componentName, runId).payload
    case SubscribeCurrentState(componentType, componentName, stateNames, maxFrequency) =>
      gatewayApi.subscribeCurrentState(componentType, componentName, stateNames, maxFrequency).resultPayloads

    case Subscribe(eventKeys, maxFrequency) => gatewayApi.subscribe(eventKeys, maxFrequency).resultPayloads
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      gatewayApi.pSubscribe(subsystem, maxFrequency, pattern).resultPayloads
  }
}
