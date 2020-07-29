package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse.NoMachineFoundForSubsystems

class AgentAllocator {
  def allocate(
      provisionConfig: ProvisionConfig,
      machines: List[AkkaLocation]
  ): Either[NoMachineFoundForSubsystems, List[(Prefix, AkkaLocation)]] = {
    val subsystemMachines: Map[Subsystem, List[AkkaLocation]] = machines.groupBy(_.prefix.subsystem)

    val allocatedPrefixesE = provisionConfig.config.toList.map {
      case (subsystem, count) => allocate(subsystem, count, subsystemMachines).map(Right(_)).getOrElse(Left(subsystem))
    }
    allocatedPrefixesE.sequence.map(_.flatten).left.map(x => NoMachineFoundForSubsystems(x.toSet))
  }

  private def allocate(
      subsystem: Subsystem,
      count: Int,
      subsystemMachines: Map[Subsystem, List[AkkaLocation]]
  ): Option[Map[Prefix, AkkaLocation]] = {
    val prefixes = configToSeqComps(subsystem, count)
    subsystemMachines.get(subsystem).map(roundRobinOn(_, prefixes))
  }
// todo: think on the naming of seq comps
  private def configToSeqComps(subsystem: Subsystem, noOfSeqComps: Int) =
    (1 to noOfSeqComps).map(i => Prefix(subsystem, s"${subsystem}_$i"))

  private def roundRobinOn(machines: List[AkkaLocation], prefixes: Seq[Prefix]) = prefixes.zip(cycle(machines: _*)).toMap

  private def cycle[T](elems: T*): LazyList[T] = LazyList.continually(elems).flatten
}
