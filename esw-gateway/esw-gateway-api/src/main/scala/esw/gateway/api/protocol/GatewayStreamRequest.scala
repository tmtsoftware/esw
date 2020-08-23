package esw.gateway.api.protocol

import csw.command.api.messages.CommandServiceStreamRequest
import csw.location.api.models.ComponentId
import csw.params.events.EventKey
import csw.prefix.models.Subsystem
import esw.ocs.api.protocol.SequencerStreamRequest
import msocket.api.Labelled

sealed trait GatewayStreamRequest

object GatewayStreamRequest {
  case class ComponentCommand(componentId: ComponentId, command: CommandServiceStreamRequest) extends GatewayStreamRequest
  case class SequencerCommand(componentId: ComponentId, command: SequencerStreamRequest)      extends GatewayStreamRequest
  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None)            extends GatewayStreamRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*")
      extends GatewayStreamRequest

  private val commandMsgLabel             = "command_msg"
  private val sequencerMsgLabel           = "sequencer_msg"
  private val subscribedEventKeysLabel    = "subscribed_event_keys"
  private val subscribedEventPatternLabel = "subscribed_pattern"
  private val subsystemLabel              = "subsystem"

  private val labelNames =
    List(
      commandMsgLabel,
      sequencerMsgLabel,
      subscribedEventKeysLabel,
      subscribedEventPatternLabel,
      subsystemLabel
    )

  implicit val websocketRequestLabelled: Labelled[GatewayStreamRequest] =
    Labelled.make(labelNames) {
      case ComponentCommand(_, command) => Map(commandMsgLabel -> Labelled.createLabel(command))
      case SequencerCommand(_, command) => Map(sequencerMsgLabel -> Labelled.createLabel(command))
      case Subscribe(eventKeys, _)      => Map(subscribedEventKeysLabel -> eventKeys.map(_.key).mkString("_"))
      case SubscribeWithPattern(subsystem, _, pattern) =>
        Map(subsystemLabel -> subsystem.name, subscribedEventPatternLabel -> pattern)
    }

  private[gateway] def createLabel(keys: Set[EventKey]) = keys.map(_.key).mkString("_")
}
