package esw.agent.service.api.models

import csw.location.api.models.{PekkoLocation, ComponentId}
import esw.agent.service.api.AgentPekkoSerializable
/*
 * These are response messages(models) of agent service.
 * These are being used in http communication.
 */
sealed trait AgentResponse extends AgentPekkoSerializable

sealed trait SpawnResponse           extends AgentResponse
sealed trait SpawnContainersResponse extends AgentResponse
sealed trait KillResponse            extends AgentResponse

case object Spawned                                   extends SpawnResponse
case object Killed                                    extends KillResponse
case class Completed(res: Map[String, SpawnResponse]) extends SpawnContainersResponse
case class Failed(msg: String)                        extends SpawnResponse with KillResponse with SpawnContainersResponse

case class SequenceComponentStatus(seqCompId: ComponentId, sequencerLocation: Option[PekkoLocation])
case class AgentStatus(agentId: ComponentId, seqCompsStatus: List[SequenceComponentStatus])

sealed trait AgentStatusResponse extends AgentPekkoSerializable

object AgentStatusResponse {
  case class Success(agentStatus: List[AgentStatus], seqCompsWithoutAgent: List[SequenceComponentStatus])
      extends AgentStatusResponse

  sealed trait Failure                            extends AgentStatusResponse
  case class LocationServiceError(reason: String) extends Failure
}
