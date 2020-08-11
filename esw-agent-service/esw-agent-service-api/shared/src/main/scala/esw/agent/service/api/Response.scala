package esw.agent.service.api

import csw.location.api.models.ComponentId

sealed trait Response extends AgentAkkaSerializable

sealed trait SpawnResponse extends Response
sealed trait KillResponse  extends Response

case object Spawned            extends SpawnResponse
case object Killed             extends KillResponse
case class Failed(msg: String) extends SpawnResponse with KillResponse

sealed trait ComponentStatus extends Response

object ComponentStatus {
  case object Initializing extends ComponentStatus
  case object Running      extends ComponentStatus
  case object Stopping     extends ComponentStatus
  case object NotAvailable extends ComponentStatus
}

case class AgentStatus(statuses: Map[ComponentId, ComponentStatus]) extends Response
