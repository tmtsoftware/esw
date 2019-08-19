package esw.gateway.server.routes.restless.messages

import csw.alarm.models.AlarmSeverity
import csw.location.models.ComponentType
import csw.params.commands.ControlCommand
import csw.params.core.models.Subsystem
import csw.params.events.Event

sealed trait RequestMsg

object RequestMsg {

  case class CommandMsg(componentType: ComponentType, componentName: String, command: ControlCommand, action: CommandAction)
      extends RequestMsg

  case class PublishEventMsg(event: Event)       extends RequestMsg
  case class GetEventMsg(eventKeys: Set[String]) extends RequestMsg
  case class SetAlarmSeverityMsg(subsystem: Subsystem, componentName: String, alarmName: String, severity: AlarmSeverity)
      extends RequestMsg
}
