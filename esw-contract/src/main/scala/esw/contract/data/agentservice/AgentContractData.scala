package esw.contract.data.agentservice

import java.nio.file.Path

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.HttpConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.models.{Failed, KillResponse, Killed, SpawnResponse, Spawned}
import esw.agent.service.api.protocol.AgentServiceRequest.{KillComponent, SpawnSequenceComponent, SpawnSequenceManager}

trait AgentContractData {
  val spawned: SpawnResponse = Spawned
  val killed: KillResponse   = Killed
  val failed: Failed         = Failed("Spawn failed")

  private val agentPrefix: Prefix      = Prefix(ESW, "agent1")
  private val seqCompPrefix: Prefix    = Prefix(ESW, "comp")
  private val componentId: ComponentId = ComponentId(seqCompPrefix, SequenceComponent)

  val spawnSequenceComponent: SpawnSequenceComponent = SpawnSequenceComponent(seqCompPrefix, "component_name", Some("1.0.0"))
  val killComponent: KillComponent                   = KillComponent(componentId)
  val spawnSequenceManager: SpawnSequenceManager =
    SpawnSequenceManager(agentPrefix, Path.of("/somePath"), isConfigLocal = true, Some("1.0.0"))
}
