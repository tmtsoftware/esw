package esw.gateway.server.routes.restless.messages

sealed trait EventErrorMessage
sealed trait AlarmErrorMessage
sealed trait CommandErrorMessage

case class SetAlarmSeverityFailure(msg: String) extends AlarmErrorMessage

case class InvalidComponent(msg: String) extends CommandErrorMessage

case class EmptyEventKeys(msg: String = "Request is missing event key") extends EventErrorMessage
case class InvalidMaxFrequency(msg: String = "Max frequency should be greater than zero")
    extends EventErrorMessage
    with CommandErrorMessage
