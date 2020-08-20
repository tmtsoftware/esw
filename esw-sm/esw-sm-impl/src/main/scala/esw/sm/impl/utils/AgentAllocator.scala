package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse.CouldNotFindMachines
import esw.sm.impl.utils.AgentAllocator.AllocationResponse
import esw.sm.impl.utils.Types._

class AgentAllocator {
  def allocate(provisionConfig: ProvisionConfig, machines: List[AkkaLocation]): AllocationResponse = {
    val allocationResult = provisionConfig.agentToSeqCompMapping.map {
      case (agentPrefix, seqCompPrefix) => machines.find(_.prefix == agentPrefix).map((_, seqCompPrefix)).toRight(agentPrefix)
    }.sequence
    allocationResult.left.map(p => CouldNotFindMachines(p.toSet))
  }
}

object AgentAllocator {
  type AllocationResponse = Either[CouldNotFindMachines, List[(AgentLocation, SeqCompPrefix)]]
}
