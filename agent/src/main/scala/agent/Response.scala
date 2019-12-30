package agent

sealed trait Response

object Response {
  case object Spawned            extends Response
  case class Failed(msg: String) extends Response
}
