package esw.gateway.server.routes.restless.messages

import csw.alarm.models.AlarmSeverity
import csw.location.models.ComponentType
import csw.params.commands.ControlCommand
import csw.params.core.models.Subsystem
import csw.params.events.Event

sealed trait HttpRequestMsg

object HttpRequestMsg {

  case class CommandMsg(componentType: ComponentType, componentName: String, command: ControlCommand, action: CommandAction)
      extends HttpRequestMsg
  case class PublishEventMsg(event: Event) extends HttpRequestMsg
  //fixme: Add codec for EventKey in CSW and use Set[EventKey]
  case class GetEventMsg(eventKeys: Set[String]) extends HttpRequestMsg
  case class SetAlarmSeverityMsg(subsystem: Subsystem, componentName: String, alarmName: String, severity: AlarmSeverity)
      extends HttpRequestMsg
}
