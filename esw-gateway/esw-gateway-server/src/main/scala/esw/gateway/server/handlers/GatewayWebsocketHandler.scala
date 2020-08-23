package esw.gateway.server.handlers

import csw.command.api.messages.CommandServiceStreamRequest
import csw.command.client.handlers.CommandServiceStreamRequestHandler
import csw.location.api.models.ComponentId
import esw.gateway.api.EventApi
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.GatewayStreamRequest
import esw.gateway.api.protocol.GatewayStreamRequest.{ComponentCommand, SequencerCommand, Subscribe, SubscribeWithPattern}
import esw.gateway.server.utils.Resolver
import esw.ocs.api.protocol.SequencerStreamRequest
import esw.ocs.handler.SequencerWebsocketHandler
import msocket.api.{StreamRequestHandler, StreamResponse}

import scala.concurrent.{ExecutionContext, Future}

class GatewayWebsocketHandler(resolver: Resolver, eventApi: EventApi)(implicit ec: ExecutionContext)
    extends StreamRequestHandler[GatewayStreamRequest] {

  override def handle(request: GatewayStreamRequest): Future[StreamResponse] =
    request match {
      case ComponentCommand(componentId, command)                 => onComponentCommand(componentId, command)
      case SequencerCommand(componentId, command)                 => onSequencerCommand(componentId, command)
      case Subscribe(eventKeys, maxFrequency)                     => stream(eventApi.subscribe(eventKeys, maxFrequency))
      case SubscribeWithPattern(subsystem, maxFrequency, pattern) => stream(eventApi.pSubscribe(subsystem, maxFrequency, pattern))
    }

  private def onComponentCommand(componentId: ComponentId, command: CommandServiceStreamRequest): Future[StreamResponse] = {
    resolver.commandService(componentId).flatMap(new CommandServiceStreamRequestHandler(_).handle(command))
  }

  private def onSequencerCommand(componentId: ComponentId, command: SequencerStreamRequest): Future[StreamResponse] = {
    resolver.sequencerCommandService(componentId).flatMap(new SequencerWebsocketHandler(_).handle(command))
  }
}
