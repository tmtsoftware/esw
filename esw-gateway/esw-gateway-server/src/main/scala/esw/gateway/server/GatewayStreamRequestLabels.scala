package esw.gateway.server

import csw.params.events.EventKey
import esw.gateway.api.protocol.GatewayStreamRequest
import esw.gateway.api.protocol.GatewayStreamRequest.{ComponentCommand, SequencerCommand, Subscribe, SubscribeWithPattern}
import msocket.jvm.metrics.Labelled

object GatewayStreamRequestLabels extends GatewayStreamRequestLabels

trait GatewayStreamRequestLabels {
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
