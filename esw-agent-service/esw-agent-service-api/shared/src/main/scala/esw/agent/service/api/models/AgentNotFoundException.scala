package esw.agent.service.api.models

case class AgentNotFoundException(msg: String) extends RuntimeException(msg)
