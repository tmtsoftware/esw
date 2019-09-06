package esw.gateway.api.messages

sealed trait CommandError
case class InvalidComponent(msg: String) extends CommandError

trait SingletonError {
  def msg: String
}

sealed trait EventError
case object EmptyEventKeys extends EventError with SingletonError {
  def msg = "Request is missing event key"
}
case object EventServerNotAvailable extends EventError with SingletonError {
  def msg = "Request is missing event key"
}

case object InvalidMaxFrequency extends EventError with CommandError with SingletonError {
  def msg = "Max frequency should be greater than zero"
}

case class SetAlarmSeverityFailure(msg: String)
