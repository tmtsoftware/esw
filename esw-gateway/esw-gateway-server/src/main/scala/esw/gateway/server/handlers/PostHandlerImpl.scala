package esw.gateway.server.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.admin.api.AdminService
import csw.command.api.messages.CommandServiceHttpMessage
import csw.command.client.handlers.CommandServiceHttpHandlers
import csw.location.models.ComponentId
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.{AlarmApi, EventApi, LoggingApi}
import esw.gateway.server.utils.Resolver
import esw.ocs.api.protocol.SequencerPostRequest
import esw.ocs.handler.SequencerPostHandler
import kamon.instrumentation.akka.http.TracingDirectives
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class PostHandlerImpl(
    alarmApi: AlarmApi,
    resolver: Resolver,
    eventApi: EventApi,
    loggingApi: LoggingApi,
    adminApi: AdminService
) extends MessageHandler[PostRequest, Route]
    with GatewayCodecs
    with ServerHttpCodecs
    with TracingDirectives {

  override def handle(request: PostRequest): Route = request match {
    case ComponentCommand(componentId, command) =>
      operationName(command.getClass.getSimpleName) { onComponentCommand(componentId, command) }
    case SequencerCommand(componentId, command) =>
      operationName(command.getClass.getSimpleName) { onSequencerCommand(componentId, command) }
    case PublishEvent(event) => operationName("publish-event") { complete(eventApi.publish(event)) }
    case GetEvent(eventKeys) => operationName("get-event") { complete(eventApi.get(eventKeys)) }
    case SetAlarmSeverity(alarmKey, severity) =>
      operationName("set-alarm-severity") { complete(alarmApi.setSeverity(alarmKey, severity)) }
    case Log(prefix, level, message, map) => operationName("log") { complete(loggingApi.log(prefix, level, message, map)) }
    case SetLogLevel(componentId, logLevel) =>
      operationName("set-log-level") { complete(adminApi.setLogLevel(componentId, logLevel)) }
    case GetLogMetadata(componentId) => operationName("get-log-metadata") { complete(adminApi.getLogMetadata(componentId)) }
  }

  private def onComponentCommand(componentId: ComponentId, command: CommandServiceHttpMessage): Route =
    onSuccess(resolver.resolveComponent(componentId)) { commandService =>
      new CommandServiceHttpHandlers(commandService).handle(command)
    }

  private def onSequencerCommand(componentId: ComponentId, command: SequencerPostRequest): Route =
    onSuccess(resolver.resolveSequencer(componentId)) { sequencerApi =>
      new SequencerPostHandler(sequencerApi).handle(command)
    }
}
