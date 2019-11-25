package esw.gateway.server.handlers

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import csw.command.client.handlers.CommandServiceWebsocketHandlers
import esw.gateway.api.EventApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.WebsocketRequest.{ComponentCommand, Subscribe, SubscribeWithPattern}
import esw.gateway.api.protocol.{InvalidComponent, WebsocketRequest}
import esw.gateway.server.utils.CommandServiceResolver
import msocket.api.MessageHandler
import msocket.impl.Encoding
import msocket.impl.ws.WebsocketStreamExtensions

class WebsocketHandlerImpl(commandServiceResolver: CommandServiceResolver, eventApi: EventApi, val encoding: Encoding[_])
    extends MessageHandler[WebsocketRequest, Source[Message, NotUsed]]
    with GatewayCodecs
    with WebsocketStreamExtensions {

  override def handle(request: WebsocketRequest): Source[Message, NotUsed] = request match {
    case ComponentCommand(componentId, command) =>
      Source.future(commandServiceResolver.resolve(componentId)).flatMapConcat {
        case Some(commandService) => new CommandServiceWebsocketHandlers(commandService, encoding).handle(command)
        case None                 => Source.failed(InvalidComponent(s"No component is registered with id $componentId "))
      }
    case Subscribe(eventKeys, maxFrequency)                     => stream(eventApi.subscribe(eventKeys, maxFrequency))
    case SubscribeWithPattern(subsystem, maxFrequency, pattern) => stream(eventApi.pSubscribe(subsystem, maxFrequency, pattern))
  }
}
