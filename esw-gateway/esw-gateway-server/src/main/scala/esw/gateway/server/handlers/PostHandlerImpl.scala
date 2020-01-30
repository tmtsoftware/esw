package esw.gateway.server.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.admin.api.AdminService
import csw.command.api.messages.CommandServiceHttpMessage
import csw.command.client.handlers.CommandServiceHttpHandlers
import csw.location.api.models.ComponentId
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.{AlarmApi, EventApi, LoggingApi}
import esw.gateway.server.utils.Resolver
import esw.ocs.api.protocol.SequencerPostRequest
import esw.ocs.handler.SequencerPostHandler
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class PostHandlerImpl(alarmApi: AlarmApi, resolver: Resolver, eventApi: EventApi, loggingApi: LoggingApi, adminApi: AdminService)
    extends HttpPostHandler[PostRequest]
    with ServerHttpCodecs {

  override def handle(request: PostRequest): Route = request match {
    case ComponentCommand(componentId, command) => onComponentCommand(componentId, command)
    case SequencerCommand(componentId, command) => onSequencerCommand(componentId, command)
    case PublishEvent(event)                    => complete(eventApi.publish(event))
    case GetEvent(eventKeys)                    => complete(eventApi.get(eventKeys))
    case SetAlarmSeverity(alarmKey, severity)   => complete(alarmApi.setSeverity(alarmKey, severity))
    case Log(prefix, level, message, map)       => complete(loggingApi.log(prefix, level, message, map))
    case SetLogLevel(componentId, logLevel)     => complete(adminApi.setLogLevel(componentId, logLevel))
    case GetLogMetadata(componentId)            => complete(adminApi.getLogMetadata(componentId))
  }

  private def onComponentCommand(componentId: ComponentId, command: CommandServiceHttpMessage): Route =
    onSuccess(resolver.commandService(componentId)) { commandService =>
      new CommandServiceHttpHandlers(commandService).handle(command)
    }

  private def onSequencerCommand(componentId: ComponentId, command: SequencerPostRequest): Route =
    onSuccess(resolver.sequencerCommandService(componentId)) { sequencerApi =>
      new SequencerPostHandler(sequencerApi).handle(command)
    }
}
