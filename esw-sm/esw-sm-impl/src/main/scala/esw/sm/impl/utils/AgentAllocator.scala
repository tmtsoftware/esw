/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse.CouldNotFindMachines
import esw.sm.impl.utils.AgentAllocator.AllocationResponse
import esw.sm.impl.utils.Types.*

class AgentAllocator {
  // agent prefix to sequence component prefix mapping is input over here. This method's responsibility is to allocate
  // actual agent locations to sequence components prefix as per mapping.
  // In case of success it return mapping of agent location to sequence component prefix else error of CouldNotFindMachines
  def allocate(provisionConfig: ProvisionConfig, machines: List[AkkaLocation]): AllocationResponse = {
    val allocationResult = provisionConfig.agentToSeqCompMapping.map { case (agentPrefix, seqCompPrefix) =>
      machines.find(_.prefix == agentPrefix).map((_, seqCompPrefix)).toRight(agentPrefix)
    }.sequence
    allocationResult.left.map(p => CouldNotFindMachines(p.toSet))
  }
}

object AgentAllocator {
  type AllocationResponse = Either[CouldNotFindMachines, List[(AgentLocation, SeqCompPrefix)]]
}
