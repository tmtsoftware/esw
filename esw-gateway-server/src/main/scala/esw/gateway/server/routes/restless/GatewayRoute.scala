package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.location.client.HttpCodecs
import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.messages.GatewayRequest
import esw.gateway.server.routes.restless.messages.GatewayRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}

class GatewayRoute(gatewayApi: GatewayApi) extends RestlessCodecs with HttpCodecs {

  val route: Route = post {
    path("gateway") {
      entity(as[GatewayRequest]) {

        case CommandRequest(componentType, componentName, command, action) =>
          complete(gatewayApi.process(componentType, componentName, command, action))

        case PublishEvent(event) => complete(gatewayApi.publish(event))
        case GetEvent(eventKeys) => complete(gatewayApi.get(eventKeys))

        case SetAlarmSeverity(subsystem, componentName, alarmName, severity) =>
          complete(gatewayApi.setSeverity(subsystem, componentName, alarmName, severity))
      }
    }
  }

}
