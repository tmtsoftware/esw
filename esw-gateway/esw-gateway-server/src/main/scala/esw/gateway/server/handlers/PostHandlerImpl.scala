package esw.gateway.server.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.{GetEvent, Oneway, PublishEvent, SetAlarmSeverity, Submit, Validate}
import esw.gateway.api.{AlarmApi, CommandApi, EventApi}
import mscoket.impl.HttpCodecs
import msocket.api.MessageHandler

class PostHandlerImpl(alarmApi: AlarmApi, commandApi: CommandApi, eventApi: EventApi)
    extends MessageHandler[PostRequest, StandardRoute]
    with GatewayCodecs
    with HttpCodecs {

  override def handle(request: PostRequest): StandardRoute = request match {
    case Submit(componentId, command)         => complete(commandApi.submit(componentId, command))
    case Oneway(componentId, command)         => complete(commandApi.oneway(componentId, command))
    case Validate(componentId, command)       => complete(commandApi.validate(componentId, command))
    case PublishEvent(event)                  => complete(eventApi.publish(event))
    case GetEvent(eventKeys)                  => complete(eventApi.get(eventKeys))
    case SetAlarmSeverity(alarmKey, severity) => complete(alarmApi.setSeverity(alarmKey, severity))
  }
}
