package esw.agent.api

sealed trait Response extends AgentAkkaSerializable

object Response {
  case object Ok                 extends Response
  case class Failed(msg: String) extends Response
}
