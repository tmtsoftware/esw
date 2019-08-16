package esw.gateway.server.routes.restless

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.params.commands.CommandResponse.Error
import esw.gateway.server.routes.restless.WebSocketMsg.QueryCommandMsg
import esw.http.core.utils.CswContext
import msocket.core.api.Payload
import msocket.core.api.ToResponse.FutureToPayload
import msocket.core.server.ServerSocket

import scala.concurrent.duration.DurationLong

class RestlessServerSocket(cswCtx: CswContext) extends ServerSocket[WebSocketMsg] with RestlessCodecs {
  import cswCtx._
  import actorRuntime.typedSystem.executionContext

  override def requestStream(request: WebSocketMsg): Source[Payload[_], NotUsed] = request match {
    case QueryCommandMsg(componentType, componentName, runId) =>
      componentFactory
        .commandService(componentName, componentType)
        .flatMap(_.queryFinal(runId)(Timeout(100.hours)))
        .recover {
          case ex: Exception =>
            Error(runId, ex.getMessage)
        }
        .payload
  }
}
