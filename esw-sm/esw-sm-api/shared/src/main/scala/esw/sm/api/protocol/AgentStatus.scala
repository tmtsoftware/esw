package esw.sm.api.protocol

import csw.location.api.models.{AkkaLocation, ComponentId}

object AgentStatus {
  type SequenceComponentStatus = Map[ComponentId, Option[AkkaLocation]]
  type AgentStatus             = Map[ComponentId, SequenceComponentStatus]
}
