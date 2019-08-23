package esw.gateway.server2

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayWebsocketRequest
import esw.gateway.api.messages.GatewayWebsocketRequest.{QueryFinal, Subscribe, SubscribeCurrentState, SubscribeWithPattern}
import esw.gateway.api.{CommandServiceApi, EventServiceApi}
import mscoket.impl.ToPayload._
import msocket.api.{Payload, ServerSocket}

import scala.concurrent.ExecutionContext

class GatewayServerSocket(commandServiceApi: CommandServiceApi, eventServiceApi: EventServiceApi)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends ServerSocket[GatewayWebsocketRequest]
    with RestlessCodecs {

  override def requestStream(request: GatewayWebsocketRequest): Source[Payload[_], NotUsed] = request match {
    case QueryFinal(componentId, runId) =>
      commandServiceApi.queryFinal(componentId, runId).payload
    case SubscribeCurrentState(componentId, stateNames, maxFrequency) =>
      commandServiceApi.subscribeCurrentState(componentId, stateNames, maxFrequency).resultPayloads

    case Subscribe(eventKeys, maxFrequency) => eventServiceApi.subscribe(eventKeys, maxFrequency).resultPayloads
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
      eventServiceApi.pSubscribe(subsystem, maxFrequency, pattern).resultPayloads
  }
}
