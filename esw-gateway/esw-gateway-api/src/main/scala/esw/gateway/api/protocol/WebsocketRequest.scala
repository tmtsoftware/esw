package esw.gateway.api.protocol

import csw.command.api.messages.CommandServiceWebsocketMessage
import csw.location.api.models.ComponentId
import csw.params.events.EventKey
import csw.prefix.models.Subsystem
import esw.ocs.api.protocol.SequencerWebsocketRequest
import msocket.api.Labelled

sealed trait WebsocketRequest

object WebsocketRequest {
  case class ComponentCommand(componentId: ComponentId, command: CommandServiceWebsocketMessage) extends WebsocketRequest
  case class SequencerCommand(componentId: ComponentId, command: SequencerWebsocketRequest)      extends WebsocketRequest
  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None)               extends WebsocketRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*")
      extends WebsocketRequest

  private val commandMsgLabelName   = "command_msg"
  private val sequencerMsgLabelName = "sequencer_msg"
  implicit val websocketRequestLabelled: Labelled[WebsocketRequest] =
    Labelled.make(List(commandMsgLabelName, sequencerMsgLabelName)) {
      case ComponentCommand(_, command) => Map(commandMsgLabelName   -> createLabel(command))
      case SequencerCommand(_, command) => Map(sequencerMsgLabelName -> createLabel(command))
    }

  private[gateway] def createLabel[A](obj: A): String = {
    val name = obj.getClass.getSimpleName
    if (name.endsWith("$")) name.dropRight(1) else name
  }
}
