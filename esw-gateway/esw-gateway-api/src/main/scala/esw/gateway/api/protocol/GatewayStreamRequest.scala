package esw.gateway.api.protocol

import csw.command.api.messages.CommandServiceStreamRequest
import csw.location.api.models.ComponentId
import csw.params.events.EventKey
import csw.prefix.models.Subsystem
import esw.ocs.api.protocol.SequencerStreamRequest

sealed trait GatewayStreamRequest

object GatewayStreamRequest {
  case class ComponentCommand(componentId: ComponentId, command: CommandServiceStreamRequest) extends GatewayStreamRequest
  case class SequencerCommand(componentId: ComponentId, command: SequencerStreamRequest)      extends GatewayStreamRequest
  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None)            extends GatewayStreamRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*")
      extends GatewayStreamRequest
}
