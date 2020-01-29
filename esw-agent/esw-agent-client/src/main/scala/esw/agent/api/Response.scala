package esw.agent.api

import csw.location.models.ComponentId

sealed trait Response extends AgentAkkaSerializable

sealed trait SpawnResponse extends Response
sealed trait KillResponse  extends Response

case object Spawned                    extends SpawnResponse
case class Killed(forcefully: Boolean) extends KillResponse
case class Failed(msg: String)         extends SpawnResponse with KillResponse

object Killed {
  val killedGracefully: Killed = Killed(false)
  val killedForcefully: Killed = Killed(true)
}

sealed trait ComponentStatus extends Response

object ComponentStatus {
  case object Initializing extends ComponentStatus
  case object Running      extends ComponentStatus
  case object Stopping     extends ComponentStatus
  case object NotAvailable extends ComponentStatus
}

case class AgentStatus(componentStatus: Map[ComponentId, ComponentStatus]) extends Response
