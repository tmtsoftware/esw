package esw.gateway.api.messages

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.models.ComponentId
import csw.params.commands.ControlCommand
import csw.params.events.{Event, EventKey}

sealed trait GatewayHttpRequest

object GatewayHttpRequest {

  case class CommandRequest(componentId: ComponentId, command: ControlCommand, action: CommandAction) extends GatewayHttpRequest
  case class PublishEvent(event: Event)                                                               extends GatewayHttpRequest
  case class GetEvent(eventKeys: Set[EventKey])                                                       extends GatewayHttpRequest
  case class SetAlarmSeverity(alarmKey: AlarmKey, severity: AlarmSeverity)                            extends GatewayHttpRequest
}
