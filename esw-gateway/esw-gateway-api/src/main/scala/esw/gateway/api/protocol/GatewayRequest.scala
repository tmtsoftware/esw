package esw.gateway.api.protocol

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.command.api.messages.CommandServiceRequest
import csw.location.api.models.ComponentId
import csw.logging.models.Level
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Prefix
import esw.ocs.api.protocol.SequencerRequest
import msocket.api.Labelled

sealed trait GatewayRequest

object GatewayRequest {
  case class ComponentCommand(componentId: ComponentId, command: CommandServiceRequest)                 extends GatewayRequest
  case class SequencerCommand(componentId: ComponentId, command: SequencerRequest)                      extends GatewayRequest
  case class PublishEvent(event: Event)                                                                 extends GatewayRequest
  case class GetEvent(eventKeys: Set[EventKey])                                                         extends GatewayRequest
  case class SetAlarmSeverity(alarmKey: AlarmKey, severity: AlarmSeverity)                              extends GatewayRequest
  case class Log(prefix: Prefix, level: Level, message: String, metadata: Map[String, Any] = Map.empty) extends GatewayRequest
  case class SetLogLevel(componentId: ComponentId, level: Level)                                        extends GatewayRequest
  case class GetLogMetadata(componentId: ComponentId)                                                   extends GatewayRequest

  private val commandMsgLabelName   = "command_msg"
  private val sequencerMsgLabelName = "sequencer_msg"
  private val labelNames            = List(commandMsgLabelName, sequencerMsgLabelName)

  implicit val postRequestLabelled: Labelled[GatewayRequest] = Labelled.make(labelNames) {
    case ComponentCommand(_, command) => Map(commandMsgLabelName -> createLabel(command))
    case SequencerCommand(_, command) => Map(sequencerMsgLabelName -> createLabel(command))
  }

  private[gateway] def createLabel[A](obj: A): String = {
    val name = obj.getClass.getSimpleName
    if (name.endsWith("$")) name.dropRight(1) else name
  }
}
