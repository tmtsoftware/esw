package esw.contract.data.sequencemanager

import java.net.URI

import csw.contract.data.command.CommandData
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.models.AgentStatusResponses.{AgentSeqCompsStatus, SequenceComponentStatus}
import esw.sm.api.models.{AgentProvisionConfig, ProvisionConfig}
import esw.sm.api.protocol.SequenceManagerPostRequest
import esw.sm.api.protocol.SequenceManagerPostRequest._

trait SequenceManagerData extends CommandData {
  private val agentPrefix                              = Prefix(ESW, "agent")
  private val seqCompPrefix                            = Prefix(ESW, "seq_comp")
  val sequencerPrefix: Prefix                          = Prefix(ESW, "DarkNight")
  val obsMode: ObsMode                                 = ObsMode("DarkNight")
  val componentId: ComponentId                         = ComponentId(sequencerPrefix, Sequencer)
  val agentComponentId: ComponentId                    = ComponentId(agentPrefix, Machine)
  val seqCompComponentId: ComponentId                  = ComponentId(seqCompPrefix, SequenceComponent)
  val akkaLocation: AkkaLocation                       = AkkaLocation(AkkaConnection(componentId), new URI("uri"))
  val sequenceComponentStatus: SequenceComponentStatus = SequenceComponentStatus(seqCompComponentId, Some(akkaLocation))
  val agentSeqCompsStatus: AgentSeqCompsStatus         = AgentSeqCompsStatus(agentComponentId, List(sequenceComponentStatus))
  val agentProvisionConfig: AgentProvisionConfig       = AgentProvisionConfig(agentPrefix, 3)
  val provisionConfig: ProvisionConfig                 = ProvisionConfig(List(agentProvisionConfig))

  val configure: Configure                                              = Configure(obsMode)
  val provision: Provision                                              = Provision(provisionConfig)
  val getRunningObsModes: GetRunningObsModes.type                       = GetRunningObsModes
  val startSequencer: StartSequencer                                    = StartSequencer(ESW, obsMode)
  val restartSequencer: RestartSequencer                                = RestartSequencer(ESW, obsMode)
  val shutdownSequencer: ShutdownSequencer                              = ShutdownSequencer(ESW, obsMode)
  val shutdownSubsystemSequencers: ShutdownSubsystemSequencers          = ShutdownSubsystemSequencers(ESW)
  val shutdownObsModeSequencers: ShutdownObsModeSequencers              = ShutdownObsModeSequencers(obsMode)
  val shutdownAllSequencers: ShutdownAllSequencers.type                 = ShutdownAllSequencers
  val spawnSequenceComponent: SpawnSequenceComponent                    = SpawnSequenceComponent(agentPrefix, "seq_comp")
  val shutdownSequenceComponent: ShutdownSequenceComponent              = ShutdownSequenceComponent(seqCompPrefix)
  val shutdownAllSequenceComponents: ShutdownAllSequenceComponents.type = ShutdownAllSequenceComponents
  val getAgentStatus: SequenceManagerPostRequest.GetAgentStatus.type    = GetAgentStatus
}
