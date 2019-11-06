package esw.gateway.server.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.{AlarmApi, CommandApi, EventApi, LoggingApi}
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class PostHandlerImpl(
    alarmApi: AlarmApi,
    commandApi: CommandApi,
    eventApi: EventApi,
    loggingApi: LoggingApi
) extends MessageHandler[PostRequest, Route]
    with GatewayCodecs
    with ServerHttpCodecs {

  override def handle(request: PostRequest): Route = request match {
    case Submit(componentId, command)         => complete(commandApi.submit(componentId, command))
    case Oneway(componentId, command)         => complete(commandApi.oneway(componentId, command))
    case Validate(componentId, command)       => complete(commandApi.validate(componentId, command))
    case PublishEvent(event)                  => complete(eventApi.publish(event))
    case GetEvent(eventKeys)                  => complete(eventApi.get(eventKeys))
    case SetAlarmSeverity(alarmKey, severity) => complete(alarmApi.setSeverity(alarmKey, severity))
    case Log(appName, level, message, map)    => complete(loggingApi.log(appName, level, message, map))
  }
}
