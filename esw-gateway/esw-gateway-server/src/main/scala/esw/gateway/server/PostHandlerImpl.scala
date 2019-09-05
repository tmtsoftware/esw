package esw.gateway.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.PostRequest
import esw.gateway.api.messages.PostRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import esw.gateway.api.{AlarmApi, CommandApi, EventApi}
import mscoket.impl.HttpCodecs
import msocket.api.RequestHandler

class PostHandlerImpl(alarmApi: AlarmApi, commandApi: CommandApi, eventApi: EventApi)
    extends RequestHandler[PostRequest, StandardRoute]
    with RestlessCodecs
    with HttpCodecs {

  override def handle(request: PostRequest): StandardRoute = request match {
    case CommandRequest(componentId, command, action) => complete(commandApi.process(componentId, command, action))
    case PublishEvent(event)                          => complete(eventApi.publish(event))
    case GetEvent(eventKeys)                          => complete(eventApi.get(eventKeys))
    case SetAlarmSeverity(alarmKey, severity)         => complete(alarmApi.setSeverity(alarmKey, severity))
  }
}
