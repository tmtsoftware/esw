package esw.agent.api

case class AgentNotFoundException(msg: String) extends RuntimeException(msg)
