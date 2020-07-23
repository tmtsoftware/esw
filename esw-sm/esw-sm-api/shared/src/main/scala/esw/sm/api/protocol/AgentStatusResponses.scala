package esw.sm.api.protocol

import csw.location.api.models.{AkkaLocation, ComponentId}

object AgentStatusResponses {
  case class AgentToSeqCompsMap(agentId: ComponentId, seqComps: List[ComponentId])
  case class SequenceComponentStatus(seqCompId: ComponentId, sequencerLocation: Option[AkkaLocation])
  case class AgentSeqCompsStatus(agentId: ComponentId, seqCompsStatus: List[SequenceComponentStatus])
}
