package esw.gateway.server2

import akka.NotUsed
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayWebsocketRequest
import esw.gateway.api.messages.GatewayWebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import mscoket.impl.ToPayload._
import msocket.api.{EitherCodecs, Payload, ServerSocket}

class GatewayServerSocket(gatewayContext: GatewayContext)
    extends ServerSocket[GatewayWebsocketRequest]
    with RestlessCodecs
    with EitherCodecs {

  import gatewayContext._
  import gatewayContext.cswContext.actorRuntime.{ec, mat}

  override def requestStream(request: GatewayWebsocketRequest): Source[Payload[_], NotUsed] = request match {
    case QueryFinal(componentType, componentName, runId) =>
      commandServiceApi.queryFinal(componentType, componentName, runId).payload
    case SubscribeCurrentState(componentType, componentName, stateNames, maxFrequency) =>
      commandServiceApi.subscribeCurrentState(componentType, componentName, stateNames, maxFrequency).resultPayloads

    case Subscribe(eventKeys, maxFrequency) => eventServiceApi.subscribe(eventKeys, maxFrequency).resultPayloads
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      eventServiceApi.pSubscribe(subsystem, maxFrequency, pattern).resultPayloads
  }
}
