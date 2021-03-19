package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.Prefix

object Types {
  type SeqCompPrefix   = Prefix
  type SeqCompLocation = AkkaLocation

  type AgentPrefix   = Prefix
  type AgentLocation = AkkaLocation
}
