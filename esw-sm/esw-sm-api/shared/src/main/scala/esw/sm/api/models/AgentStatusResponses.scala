package esw.sm.api.models

import csw.location.api.models.{AkkaLocation, ComponentId}

case class SequenceComponentStatus(seqCompId: ComponentId, sequencerLocation: Option[AkkaLocation])
case class AgentStatus(agentId: ComponentId, seqCompsStatus: List[SequenceComponentStatus])
