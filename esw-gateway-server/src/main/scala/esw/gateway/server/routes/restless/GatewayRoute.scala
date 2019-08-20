package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.location.client.HttpCodecs
import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.messages.GatewayMessage
import esw.gateway.server.routes.restless.messages.GatewayMessage.{
  CommandMessage,
  GetEventMessage,
  PublishEventMessage,
  SetAlarmSeverityMessage
}

class GatewayRoute(gatewayApi: GatewayApi) extends RestlessCodecs with HttpCodecs {

  val route: Route = post {
    path("gateway") {
      entity(as[GatewayMessage]) {

        case CommandMessage(componentType, componentName, command, action) =>
          complete(gatewayApi.process(componentType, componentName, command, action))

        case PublishEventMessage(event) => complete(gatewayApi.publish(event))
        case GetEventMessage(eventKeys) => complete(gatewayApi.get(eventKeys))

        case SetAlarmSeverityMessage(subsystem, componentName, alarmName, severity) =>
          complete(gatewayApi.setSeverity(subsystem, componentName, alarmName, severity))
      }
    }
  }

}
