/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.server.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.command.api.messages.CommandServiceRequest
import csw.command.client.auth.CommandRoles
import csw.command.client.handlers.CommandServiceRequestHandler
import csw.location.api.models.ComponentId
import csw.logging.models.Level
import esw.commons.auth.AuthPolicies
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest._
import esw.gateway.api.{AdminApi, AlarmApi, EventApi, LoggingApi}
import esw.gateway.server.utils.Resolver
import esw.ocs.api.protocol.SequencerRequest
import esw.ocs.handler.SequencerPostHandler
import msocket.http.post.{HttpPostHandler, ServerHttpCodecs}

/**
 * This is the Http(POST) route handler written using msocket apis for Gateway.
 * @param alarmApi - an instance of alarm api
 * @param resolver - an instance of resolver for resolving hcd/assemblies & sequencers
 * @param eventApi - an instance of event api
 * @param loggingApi - an instance of logging api
 * @param adminApi - an instance of admin api
 * @param securityDirectives - an instance of security directives
 * @param commandRoles - Map of commands & user-roles allowed to sent commands
 */
class GatewayPostHandler(
    alarmApi: AlarmApi,
    resolver: Resolver,
    eventApi: EventApi,
    loggingApi: LoggingApi,
    adminApi: AdminApi,
    securityDirectives: SecurityDirectives,
    commandRoles: CommandRoles
) extends HttpPostHandler[GatewayRequest]
    with ServerHttpCodecs {

  override def handle(request: GatewayRequest): Route =
    request match {
      case ComponentCommand(componentId, command)  => onComponentCommand(componentId, command)
      case SequencerCommand(componentId, command)  => onSequencerCommand(componentId, command)
      case PublishEvent(event)                     => complete(eventApi.publish(event))
      case GetEvent(eventKeys)                     => complete(eventApi.get(eventKeys))
      case SetAlarmSeverity(alarmKey, severity)    => complete(alarmApi.setSeverity(alarmKey, severity))
      case Log(prefix, level, message, map)        => complete(loggingApi.log(prefix, level, message, map))
      case SetLogLevel(componentId, logLevel)      => onSetLogLevel(componentId, logLevel)
      case GetLogMetadata(componentId)             => complete(adminApi.getLogMetadata(componentId))
      case Shutdown(componentId)                   => sPost(componentId, complete(adminApi.shutdown(componentId)))
      case Restart(componentId)                    => sPost(componentId, complete(adminApi.restart(componentId)))
      case GoOffline(componentId)                  => sPost(componentId, complete(adminApi.goOffline(componentId)))
      case GoOnline(componentId)                   => sPost(componentId, complete(adminApi.goOnline(componentId)))
      case GetContainerLifecycleState(prefix)      => complete(adminApi.getContainerLifecycleState(prefix))
      case GetComponentLifecycleState(componentId) => complete(adminApi.getComponentLifecycleState(componentId))
    }

  private def onSetLogLevel(componentId: ComponentId, logLevel: Level): Route =
    securityDirectives.sPost(AuthPolicies.eswUserOrSubsystemUserPolicy(componentId.prefix.subsystem))(_ =>
      complete(adminApi.setLogLevel(componentId, logLevel))
    )

  private def sPost(componentId: ComponentId, route: => Route): Route =
    securityDirectives.sPost(AuthPolicies.eswUserOrSubsystemEngPolicy(componentId.prefix.subsystem))(_ => route)

  private def onComponentCommand(componentId: ComponentId, command: CommandServiceRequest): Route =
    onSuccess(resolver.commandService(componentId)) { commandService =>
      new CommandServiceRequestHandler(commandService, securityDirectives, Some(componentId.prefix), commandRoles).handle(command)
    }

  private def onSequencerCommand(componentId: ComponentId, command: SequencerRequest): Route =
    onSuccess(resolver.sequencerCommandService(componentId)) { sequencerApi =>
      new SequencerPostHandler(sequencerApi, securityDirectives, Some(componentId.prefix)).handle(command)
    }
}
