/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.service.api.protocol

import java.nio.file.Path

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
//Http Request models for the Agent Service
sealed trait AgentServiceRequest

object AgentServiceRequest {

  case class SpawnSequenceManager(agentPrefix: Prefix, obsModeConfigPath: Path, isConfigLocal: Boolean, version: Option[String])
      extends AgentServiceRequest

  case class SpawnSequenceComponent(agentPrefix: Prefix, componentName: String, version: Option[String])
      extends AgentServiceRequest

  case class SpawnContainers(agentPrefix: Prefix, hostConfigPath: String, isConfigLocal: Boolean) extends AgentServiceRequest

  case class KillComponent(componentId: ComponentId) extends AgentServiceRequest

  case object GetAgentStatus extends AgentServiceRequest
}
