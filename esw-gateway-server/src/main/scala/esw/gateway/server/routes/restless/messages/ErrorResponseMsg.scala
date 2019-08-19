package esw.gateway.server.routes.restless.messages

sealed trait ErrorResponseMsg {
  def msg: String
}

object ErrorResponseMsg {

  case class EmptyEventKeys(msg: String = "Request is missing event key") extends ErrorResponseMsg

  case class SetAlarmSeverityFailure(msg: String)                                           extends ErrorResponseMsg
  case class InvalidComponent(msg: String)                                                  extends ErrorResponseMsg
  case class InvalidMaxFrequency(msg: String = "Max frequency should be greater than zero") extends ErrorResponseMsg
}
