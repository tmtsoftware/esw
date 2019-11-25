package esw.gateway.server.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.command.client.handlers.CommandServiceHttpHandlers
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.{AlarmApi, EventApi, LoggingApi}
import esw.gateway.server.utils.CommandServiceResolver
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class PostHandlerImpl(
    alarmApi: AlarmApi,
    commandServiceResolver: CommandServiceResolver,
    eventApi: EventApi,
    loggingApi: LoggingApi
) extends MessageHandler[PostRequest, Route]
    with GatewayCodecs
    with ServerHttpCodecs {

  override def handle(request: PostRequest): Route = request match {
    case ComponentCommand(componentId, command) =>
      onSuccess(commandServiceResolver.resolve(componentId)) {
        case Some(commandService) => new CommandServiceHttpHandlers(commandService).handle(command)
        case None                 => complete(StatusCodes.BadRequest -> s"No component is registered with id $componentId ")
      }
    case PublishEvent(event)                  => complete(eventApi.publish(event))
    case GetEvent(eventKeys)                  => complete(eventApi.get(eventKeys))
    case SetAlarmSeverity(alarmKey, severity) => complete(alarmApi.setSeverity(alarmKey, severity))
    case Log(appName, level, message, map)    => complete(loggingApi.log(appName, level, message, map))
  }
}
