package esw.sm.impl.utils

import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix

object Types {
  type SeqCompPrefix   = Prefix
  type SeqCompLocation = AkkaLocation
  type SeqCompId       = ComponentId

  type AgentPrefix   = Prefix
  type AgentLocation = AkkaLocation
  type AgentId       = ComponentId

  type SequencerLocation = AkkaLocation
}
