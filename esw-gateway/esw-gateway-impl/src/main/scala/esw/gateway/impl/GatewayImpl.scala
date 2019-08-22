package esw.gateway.impl

import esw.http.core.utils.CswContext

class GatewayImpl(val cswContext: CswContext) extends CommandGatewayImpl with AlarmGatewayImpl with EventGatewayImpl
