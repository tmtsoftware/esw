package esw.agent.service.api.models

import csw.location.api.models.ComponentId
import esw.agent.service.api.AgentAkkaSerializable

sealed trait AgentResponse extends AgentAkkaSerializable

sealed trait SpawnResponse extends AgentResponse
sealed trait KillResponse  extends AgentResponse

case object Spawned            extends SpawnResponse
case object Killed             extends KillResponse
case class Failed(msg: String) extends SpawnResponse with KillResponse

sealed trait ComponentStatus extends AgentResponse

object ComponentStatus {
  case object Initializing extends ComponentStatus
  case object Running      extends ComponentStatus
  case object Stopping     extends ComponentStatus
  case object NotAvailable extends ComponentStatus
}

case class AgentStatus(statuses: Map[ComponentId, ComponentStatus]) extends AgentResponse
