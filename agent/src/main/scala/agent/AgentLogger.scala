package agent

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

private[agent] object AgentLogger extends LoggerFactory(Prefix(ESW, "agent_app"))
