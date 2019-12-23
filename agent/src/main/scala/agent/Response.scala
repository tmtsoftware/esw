package agent

sealed trait Response

object Response {
  case object Started           extends Response
  case class Error(msg: String) extends Response
}
