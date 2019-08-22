package esw.gateway.api.messages

sealed trait CommandError
case class InvalidComponent(msg: String) extends CommandError

sealed trait EventError
case class EmptyEventKeys(msg: String = "Request is missing event key")                   extends EventError
case class InvalidMaxFrequency(msg: String = "Max frequency should be greater than zero") extends EventError with CommandError

case class SetAlarmSeverityFailure(msg: String)
