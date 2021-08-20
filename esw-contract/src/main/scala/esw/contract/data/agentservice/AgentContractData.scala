package esw.contract.data.agentservice

import java.net.URI
import java.nio.file.Path

import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.models.AgentStatusResponse.LocationServiceError
import esw.agent.service.api.models._
import esw.agent.service.api.protocol.AgentServiceRequest.{
  GetAgentStatus,
  KillComponent,
  SpawnSequenceComponent,
  SpawnSequenceManager
}

/**
 * This object contains all the Agent Service data models which will be sent on wire.
 */
trait AgentContractData {
  val spawned: SpawnResponse           = Spawned
  val killed: KillResponse             = Killed
  val failed: Failed                   = Failed("Spawn failed")
  private val agentPrefix: Prefix      = Prefix(ESW, "agent1")
  private val seqCompPrefix: Prefix    = Prefix(ESW, "comp")
  private val componentId: ComponentId = ComponentId(seqCompPrefix, SequenceComponent)
  val seqCompComponentId: ComponentId  = ComponentId(seqCompPrefix, SequenceComponent)
  val akkaLocation: AkkaLocation =
    AkkaLocation(AkkaConnection(seqCompComponentId), new URI("uri"), Metadata().add("key1", "value"))
  val agentComponentId: ComponentId                    = ComponentId(agentPrefix, Machine)
  val sequenceComponentStatus: SequenceComponentStatus = SequenceComponentStatus(seqCompComponentId, Some(akkaLocation))
  val agentSeqCompsStatus: AgentStatus                 = AgentStatus(agentComponentId, List(sequenceComponentStatus))
  val agentStatusSuccess: AgentStatusResponse.Success =
    AgentStatusResponse.Success(List(agentSeqCompsStatus), List(sequenceComponentStatus))
  val agentStatusFailure: LocationServiceError = LocationServiceError("location service error")

  val spawnSequenceComponent: SpawnSequenceComponent = SpawnSequenceComponent(seqCompPrefix, "component_name", Some("1.0.0"))
  val killComponent: KillComponent                   = KillComponent(componentId)
  val spawnSequenceManager: SpawnSequenceManager =
    SpawnSequenceManager(agentPrefix, Path.of("/somePath"), isConfigLocal = true, Some("1.0.0"))
  val getAgentStatus: GetAgentStatus.type = GetAgentStatus
}
