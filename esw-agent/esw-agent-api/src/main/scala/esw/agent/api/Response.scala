package esw.agent.api

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
