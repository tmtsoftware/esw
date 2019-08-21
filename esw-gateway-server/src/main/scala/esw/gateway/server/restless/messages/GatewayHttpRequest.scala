package esw.gateway.server.restless.messages

import csw.alarm.models.AlarmSeverity
import csw.location.models.ComponentType
import csw.params.commands.ControlCommand
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}

sealed trait GatewayHttpRequest

object GatewayHttpRequest {

  case class CommandRequest(componentType: ComponentType, componentName: String, command: ControlCommand, action: CommandAction)
      extends GatewayHttpRequest
  case class PublishEvent(event: Event)         extends GatewayHttpRequest
  case class GetEvent(eventKeys: Set[EventKey]) extends GatewayHttpRequest
  case class SetAlarmSeverity(subsystem: Subsystem, componentName: String, alarmName: String, severity: AlarmSeverity)
      extends GatewayHttpRequest
}
