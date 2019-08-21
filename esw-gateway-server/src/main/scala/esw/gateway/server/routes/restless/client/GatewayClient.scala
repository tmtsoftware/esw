package esw.gateway.server.routes.restless.client

import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.messages.GatewayWebsocketRequest
import esw.http.core.utils.CswContext
import msocket.core.client.ClientSocket

class GatewayClient(val cswCtx: CswContext, val socket: ClientSocket[GatewayWebsocketRequest])
    extends GatewayHttpClient
    with GatewayWebsocketClient
    with GatewayApi
