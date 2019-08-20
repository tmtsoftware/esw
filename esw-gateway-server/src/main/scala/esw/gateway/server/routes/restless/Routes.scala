package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation._enhanceRouteWithConcatenation
import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import msocket.core.server.WsServerFlow

class Routes(val gatewayApi: GatewayApi) extends RestlessCodecs {
  private val gatewayRoute   = new GatewayRoute(gatewayApi)
  private val serverSocket   = new RestlessServerSocket(gatewayApi)
  private val flow           = new WsServerFlow(serverSocket)
  private val websocketRoute = new WebsocketRoute(flow)

  val route: Route = gatewayRoute.route ~ websocketRoute.route
}
