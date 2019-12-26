package agent

sealed trait Response

object Response {
  case object Done              extends Response
  case class Error(msg: String) extends Response
}
