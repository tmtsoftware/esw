package esw.gateway.api.protocol

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.models.ComponentId
import csw.params.commands.ControlCommand
import csw.params.events.{Event, EventKey}

sealed trait PostRequest

object PostRequest {
  case class CommandRequest(componentId: ComponentId, command: ControlCommand, action: CommandAction) extends PostRequest
  case class PublishEvent(event: Event)                                                               extends PostRequest
  case class GetEvent(eventKeys: Set[EventKey])                                                       extends PostRequest
  case class SetAlarmSeverity(alarmKey: AlarmKey, severity: AlarmSeverity)                            extends PostRequest
}
