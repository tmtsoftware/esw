package esw.gateway.api.messages

sealed trait CommandError
case class InvalidComponent(msg: String) extends CommandError

trait SingletonError {
  def msg: String
}

sealed trait EventError
sealed trait GetEventError extends EventError

case object EmptyEventKeys extends GetEventError with SingletonError {
  def msg = "Request is missing event key"
}
case object EventServerUnavailable extends GetEventError with SingletonError {
  def msg = "Event server is unavailable"
}

case object InvalidMaxFrequency extends EventError with CommandError with SingletonError {
  def msg = "Max frequency should be greater than zero"
}

case class SetAlarmSeverityFailure(msg: String)
