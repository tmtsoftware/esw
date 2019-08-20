package esw.gateway.server.routes.restless.messages

import csw.alarm.models.AlarmSeverity
import csw.location.models.ComponentType
import csw.params.commands.ControlCommand
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}

sealed trait GatewayRequest

object GatewayRequest {

  case class CommandRequest(componentType: ComponentType, componentName: String, command: ControlCommand, action: CommandAction)
      extends GatewayRequest
  case class PublishEvent(event: Event)         extends GatewayRequest
  case class GetEvent(eventKeys: Set[EventKey]) extends GatewayRequest
  case class SetAlarmSeverity(subsystem: Subsystem, componentName: String, alarmName: String, severity: AlarmSeverity)
      extends GatewayRequest
}
