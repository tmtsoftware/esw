package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse.CouldNotFindMachines

class AgentAllocator {
  type AgentToSeqComp = (Prefix, Prefix)

  def allocate(
      provisionConfig: ProvisionConfig,
      machines: List[AkkaLocation]
  ): Either[CouldNotFindMachines, List[(AkkaLocation, Prefix)]] = {
    def mapToMachines(agentPrefix: Prefix, seqComp: Prefix): Either[Prefix, (AkkaLocation, Prefix)] =
      machines.find(_.prefix == agentPrefix).map((_, seqComp)).toRight(agentPrefix)

    val mapping = provisionConfig.config
      .groupBy(_._1.subsystem)
      .toList
      .flatMap { case (subsystem, subsystemConfig) => allocateForSubsystem(subsystem, subsystemConfig) }
      .map { case (machineId, seqComp) => mapToMachines(machineId, seqComp) }

    mapping.sequence.left.map(x => CouldNotFindMachines(x.toSet))
  }

  private def allocateForSubsystem(subsystem: Subsystem, subsystemConfig: Map[Prefix, Int]): List[AgentToSeqComp] = {
    var seqCompPrefixes = configToSeqComps(subsystem, subsystemConfig.values.sum)

    subsystemConfig.toList.flatMap {
      case (agentPrefix, count) =>
        val (allocated, remaining) = seqCompPrefixes.splitAt(count)

        seqCompPrefixes = remaining                                  // assign back remaining
        allocated.map(seqCompPrefix => (agentPrefix, seqCompPrefix)) // return the allocated
    }
  }

  private def configToSeqComps(subsystem: Subsystem, noOfSeqComps: Int) =
    (1 to noOfSeqComps).map(i => Prefix(subsystem, s"${subsystem}_$i"))

}
