package esw.gateway.api.protocol

import msocket.api.models.StreamError

case class InvalidComponent(msg: String) {
  def toStreamError = StreamError(this.getClass.getSimpleName, msg)
}

trait SingletonError {
  def msg: String
  def toStreamError = StreamError(this.getClass.getSimpleName, msg)
}

sealed trait GetEventError

case object EmptyEventKeys extends GetEventError with SingletonError {
  def msg = "Request is missing event key"

}
case object EventServerUnavailable extends GetEventError with SingletonError {
  def msg = "Event server is unavailable"
}

object InvalidMaxFrequency extends SingletonError {
  def msg = "Max frequency should be greater than zero"
}

case class SetAlarmSeverityFailure(msg: String)
