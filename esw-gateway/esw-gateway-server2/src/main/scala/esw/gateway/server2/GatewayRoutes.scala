package esw.gateway.server2

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.Materializer
import csw.location.client.HttpCodecs
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayHttpRequest
import esw.gateway.api.messages.GatewayHttpRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import esw.gateway.api.{AlarmServiceApi, CommandServiceApi, EventServiceApi}
import mscoket.impl.{Encoding, WsServerFlow}

import scala.concurrent.ExecutionContext

class GatewayRoutes(alarmServiceApi: AlarmServiceApi, commandServiceApi: CommandServiceApi, eventServiceApi: EventServiceApi)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends RestlessCodecs
    with HttpCodecs {

  private val socket       = new GatewayServerSocket(commandServiceApi, eventServiceApi)
  private val wsServerFlow = new WsServerFlow(socket)

  val route: Route = get {
    path("websocket" / Segment) { encoding =>
      handleWebSocketMessages(wsServerFlow.flow(Encoding.fromString(encoding)))
    }
  } ~
    post {
      path("gateway") {
        entity(as[GatewayHttpRequest])(httpSocket)
      }
    }

  private def httpSocket(request: GatewayHttpRequest): StandardRoute = request match {
    case CommandRequest(componentId, command, action) =>
      complete(commandServiceApi.process(componentId, command, action))

    case PublishEvent(event) => complete(eventServiceApi.publish(event))
    case GetEvent(eventKeys) => complete(eventServiceApi.get(eventKeys))

    case SetAlarmSeverity(alarmKey, severity) =>
      complete(alarmServiceApi.setSeverity(alarmKey, severity))
  }
}
