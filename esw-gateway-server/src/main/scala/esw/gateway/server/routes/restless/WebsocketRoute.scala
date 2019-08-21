package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.messages.GatewayWebsocketRequest
import msocket.core.api.Encoding
import msocket.core.server.WsServerFlow

class WebsocketRoute(wsServerFlow: WsServerFlow[GatewayWebsocketRequest]) extends RestlessCodecs {

  val route: Route =
    path("websocket" / Segment) { encoding =>
      handleWebSocketMessages(wsServerFlow.flow(Encoding.fromString(encoding)))
    }

}
