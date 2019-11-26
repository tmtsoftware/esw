package esw.gateway.api.protocol

import csw.command.api.messages.CommandServiceWebsocketMessage
import csw.location.models.ComponentId
import csw.params.core.models.Subsystem
import csw.params.events.EventKey

sealed trait WebsocketRequest

object WebsocketRequest {
  case class ComponentCommand(componentId: ComponentId, command: CommandServiceWebsocketMessage) extends WebsocketRequest
  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None)               extends WebsocketRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*")
      extends WebsocketRequest
}
