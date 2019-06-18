package esw.gateway.server.http

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}

/**
 * `GatewayConnection` is a wrapper over predefined `HttpConnection` representing gateway server. It is used to register
 * with location service.
 */
object GatewayConnection {
  val value = HttpConnection(ComponentId("GatewayServer", ComponentType.Service))
}
