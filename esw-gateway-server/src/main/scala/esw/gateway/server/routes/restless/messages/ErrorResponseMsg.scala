package esw.gateway.server.routes.restless.messages

sealed trait ErrorResponseMsg {
  def msg: String
}

object ErrorResponseMsg {

  case class NoEventKeys(msg: String = "Request is missing query parameter key") extends ErrorResponseMsg

  case class SetAlarmSeverityFailure(msg: String) extends ErrorResponseMsg
  case class InvalidComponent(msg: String)        extends ErrorResponseMsg
}
