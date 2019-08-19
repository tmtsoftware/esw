package esw.gateway.server.routes.restless

import akka.NotUsed
import akka.stream.scaladsl.Source
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.impl.{CommandServiceImpl, EventServiceImpl}
import esw.gateway.server.routes.restless.messages.WebSocketMsg
import esw.gateway.server.routes.restless.messages.WebSocketMsg.{
  CurrentStateSubscriptionCommandMsg,
  PatternSubscribeEventMsg,
  QueryCommandMsg,
  SubscribeEventMsg
}
import esw.http.core.utils.CswContext
import msocket.core.api.Payload
import msocket.core.api.ToPayload.{FutureToPayload, SourceWithErrorToPayload}
import msocket.core.server.ServerSocket

class RestlessServerSocket(cswCtx: CswContext) extends ServerSocket[WebSocketMsg] with RestlessCodecs {

  lazy val commandServiceApi: CommandServiceImpl = new CommandServiceImpl(cswCtx)
  lazy val eventServiceApi: EventServiceImpl     = new EventServiceImpl(cswCtx)

  import cswCtx.actorRuntime.mat
  import cswCtx.actorRuntime.typedSystem.executionContext

  override def requestStream(request: WebSocketMsg): Source[Payload[_], NotUsed] = request match {
    case queryCommandMsg: QueryCommandMsg => commandServiceApi.queryFinal(queryCommandMsg).payload
    case subscriptionCommandMsg: CurrentStateSubscriptionCommandMsg =>
      commandServiceApi.subscribeCurrentState(subscriptionCommandMsg).resultPayloads
    case subscribeEventMsg: SubscribeEventMsg         => eventServiceApi.subscribe(subscribeEventMsg).resultPayloads
    case pSubscribeEventMsg: PatternSubscribeEventMsg => eventServiceApi.pSubscribe(pSubscribeEventMsg).resultPayloads
  }
}
