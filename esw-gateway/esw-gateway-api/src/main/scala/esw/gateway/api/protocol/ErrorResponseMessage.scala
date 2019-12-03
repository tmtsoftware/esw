package esw.gateway.api.protocol

import msocket.api.models.ProtocolError

case class InvalidComponent(msg: String) extends RuntimeException(msg) {
  def protocolError: ProtocolError = ProtocolError(this.getClass.getSimpleName, msg)
}

trait SingletonError {
  def msg: String
  def protocolError: ProtocolError = ProtocolError(this.getClass.getSimpleName, msg)
}

sealed trait GetEventError

case object EmptyEventKeys extends GetEventError with SingletonError {
  def msg = "Request is missing event key"
}
case object EventServerUnavailable extends GetEventError with SingletonError {
  def msg = "Event server is unavailable"
}

case class InvalidMaxFrequency() extends RuntimeException("Max frequency should be greater than zero")

case class SetAlarmSeverityFailure(msg: String)
