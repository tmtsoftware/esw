package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import msocket.core.api.Encoding
import msocket.core.server.WsServerFlow

class WebsocketRoute(wsServerFlow: WsServerFlow[WebSocketMsg]) extends RestlessCodecs {

  val route: Route =
    path("websocket" / Segment) { encoding =>
      handleWebSocketMessages(wsServerFlow.flow(Encoding.fromString(encoding)))
    }

}
