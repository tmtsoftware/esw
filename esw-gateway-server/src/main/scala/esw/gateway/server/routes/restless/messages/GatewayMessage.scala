package esw.gateway.server.routes.restless.messages

import csw.alarm.models.AlarmSeverity
import csw.location.models.ComponentType
import csw.params.commands.ControlCommand
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}

sealed trait GatewayMessage

object GatewayMessage {

  case class CommandMessage(componentType: ComponentType, componentName: String, command: ControlCommand, action: CommandAction)
      extends GatewayMessage
  case class PublishEventMessage(event: Event)         extends GatewayMessage
  case class GetEventMessage(eventKeys: Set[EventKey]) extends GatewayMessage
  case class SetAlarmSeverityMessage(subsystem: Subsystem, componentName: String, alarmName: String, severity: AlarmSeverity)
      extends GatewayMessage
}
