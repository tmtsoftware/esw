package esw.gateway.server.handlers

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import csw.command.api.messages.CommandServiceWebsocketMessage
import csw.command.client.handlers.CommandServiceWebsocketHandlers
import csw.location.models.ComponentId
import esw.gateway.api.EventApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.WebsocketRequest
import esw.gateway.api.protocol.WebsocketRequest.{ComponentCommand, SequencerCommand, Subscribe, SubscribeWithPattern}
import esw.gateway.server.utils.Resolver
import esw.ocs.api.protocol.SequencerWebsocketRequest
import esw.ocs.handler.SequencerWebsocketHandler
import msocket.api.MessageHandler
import msocket.impl.Encoding
import msocket.impl.ws.WebsocketStreamExtensions

class WebsocketHandlerImpl(resolver: Resolver, eventApi: EventApi, val encoding: Encoding[_])
    extends MessageHandler[WebsocketRequest, Source[Message, NotUsed]]
    with GatewayCodecs
    with WebsocketStreamExtensions {

  override def handle(request: WebsocketRequest): Source[Message, NotUsed] = request match {
    case ComponentCommand(componentId, command)                 => onComponentCommand(componentId, command)
    case SequencerCommand(componentId, command)                 => onSequencerCommand(componentId, command)
    case Subscribe(eventKeys, maxFrequency)                     => stream(eventApi.subscribe(eventKeys, maxFrequency))
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) => stream(eventApi.pSubscribe(subsystem, maxFrequency, pattern))
  }

  private def onComponentCommand(componentId: ComponentId, command: CommandServiceWebsocketMessage): Source[Message, NotUsed] =
    Source
      .future(resolver.resolveComponent(componentId))
      .flatMapConcat(commandService => new CommandServiceWebsocketHandlers(commandService, encoding).handle(command))

  private def onSequencerCommand(componentId: ComponentId, command: SequencerWebsocketRequest): Source[Message, NotUsed] =
    Source
      .future(resolver.resolveSequencer(componentId))
      .flatMapConcat(sequencerApi => new SequencerWebsocketHandler(sequencerApi, encoding).handle(command))
}
