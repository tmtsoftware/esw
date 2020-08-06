package esw.agent.api.protocol

import java.nio.file.Path

import csw.prefix.models.Prefix

sealed trait AgentHttpMessage

case class SpawnSequenceManager(agentPrefix: Prefix, obsModeConfigPath: Path, isConfigLocal: Boolean, version: Option[String])
    extends AgentHttpMessage
case class SpawnSequenceComponent(agentPrefix: Prefix, prefix: Prefix, version: Option[String]) extends AgentHttpMessage
