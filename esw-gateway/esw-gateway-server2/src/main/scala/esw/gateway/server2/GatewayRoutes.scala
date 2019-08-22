package esw.gateway.server2

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import csw.location.client.HttpCodecs
import esw.gateway.api.GatewayApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayHttpRequest
import esw.gateway.api.messages.GatewayHttpRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import mscoket.impl.{Encoding, WsServerFlow}
import msocket.api.EitherCodecs

class GatewayRoutes(val gatewayApi: GatewayApi) extends RestlessCodecs with EitherCodecs with HttpCodecs {
  private val serverSocket = new GatewayServerSocket(gatewayApi)
  private val wsServerFlow = new WsServerFlow(serverSocket)

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
    case CommandRequest(componentType, componentName, command, action) =>
      complete(gatewayApi.process(componentType, componentName, command, action))

    case PublishEvent(event) => complete(gatewayApi.publish(event))
    case GetEvent(eventKeys) => complete(gatewayApi.get(eventKeys))

    case SetAlarmSeverity(subsystem, componentName, alarmName, severity) =>
      complete(gatewayApi.setSeverity(subsystem, componentName, alarmName, severity))
  }
}
