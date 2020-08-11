package esw.agent.service.api

case class AgentNotFoundException(msg: String) extends RuntimeException(msg)
