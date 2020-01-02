package agent.api

sealed trait Response extends AgentAkkaSerializable

object Response {
  case object Spawned            extends Response
  case class Failed(msg: String) extends Response
}
