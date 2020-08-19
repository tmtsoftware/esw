package esw.agent.service.api.models

import esw.agent.service.api.AgentAkkaSerializable

sealed trait AgentResponse extends AgentAkkaSerializable

sealed trait SpawnResponse extends AgentResponse
sealed trait KillResponse  extends AgentResponse

case object Spawned            extends SpawnResponse
case object Killed             extends KillResponse
case class Failed(msg: String) extends SpawnResponse with KillResponse
