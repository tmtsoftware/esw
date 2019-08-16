package esw.gateway.server.routes.restless

sealed trait ResponseMsg {
  def msg: String
}

object ResponseMsg {

  case object NoEventKeys extends ResponseMsg {
    val msg: String = "Request is missing query parameter key"
  }

  case class SetAlarmSeverityFailure(msg: String) extends ResponseMsg
  case class CommandActionFailure(msg: String)    extends ResponseMsg
}
