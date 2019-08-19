package esw.gateway.server.routes.restless

import akka.NotUsed
import akka.stream.scaladsl.Source
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.impl.CommandServiceImpl
import esw.gateway.server.routes.restless.messages.WebSocketMsg
import esw.gateway.server.routes.restless.messages.WebSocketMsg.{CurrentStateSubscriptionCommandMsg, QueryCommandMsg}
import esw.http.core.utils.CswContext
import msocket.core.api.Payload
import msocket.core.api.ToPayload.FutureToPayload
import msocket.core.server.ServerSocket

class RestlessServerSocket(cswCtx: CswContext) extends ServerSocket[WebSocketMsg] with RestlessCodecs {

  lazy val commandServiceApi: CommandServiceImpl = new CommandServiceImpl(cswCtx)

  import cswCtx.actorRuntime.typedSystem.executionContext

  override def requestStream(request: WebSocketMsg): Source[Payload[_], NotUsed] = request match {
    case queryCommandMsg: QueryCommandMsg      => commandServiceApi.queryFinal(queryCommandMsg).payload
    case _: CurrentStateSubscriptionCommandMsg => ???
  }
}
