package esw.gateway.api.protocol

import csw.command.api.messages.CommandServiceWebsocketMessage
import csw.location.models.ComponentId
import csw.params.events.EventKey
import csw.prefix.models.Subsystem
import esw.ocs.api.protocol.SequencerWebsocketRequest

sealed trait WebsocketRequest

object WebsocketRequest {
  case class ComponentCommand(componentId: ComponentId, command: CommandServiceWebsocketMessage) extends WebsocketRequest
  case class SequencerCommand(componentId: ComponentId, command: SequencerWebsocketRequest)      extends WebsocketRequest
  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None)               extends WebsocketRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*")
      extends WebsocketRequest
}
