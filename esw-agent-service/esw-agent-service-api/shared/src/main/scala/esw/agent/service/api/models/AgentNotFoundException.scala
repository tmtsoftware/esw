package esw.agent.service.api.models

/**
 * Model representing when the request Agent is not up & running.
 * @param msg [[java.lang.String]] - a hint containing information about agent.
 */
case class AgentNotFoundException(msg: String) extends RuntimeException(msg)
