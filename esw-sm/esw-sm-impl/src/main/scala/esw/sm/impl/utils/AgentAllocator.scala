package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.sm.api.protocol.ProvisionResponse.NoMachineFoundForSubsystems
import esw.sm.impl.config.ProvisionConfig

case class AgentAllocator(machines: List[AkkaLocation]) {
  private val subsystemMachines = machines.groupBy(_.prefix.subsystem)

  def allocate(provisionConfig: ProvisionConfig): Either[NoMachineFoundForSubsystems, List[Map[Prefix, AkkaLocation]]] = {
    val allocatedPrefixesE = provisionConfig.config.toList.map { config =>
      val (subsystem, count)     = config
      val maybeAllocatedPrefixes = allocate(subsystem, count)
      maybeAllocatedPrefixes.map(Right(_)).getOrElse(Left(subsystem))
    }
    allocatedPrefixesE.sequence.left.map(x => NoMachineFoundForSubsystems(x.toSet))
  }

  private def allocate(subsystem: Subsystem, count: Int): Option[Map[Prefix, AkkaLocation]] = {
    val prefixes = configToSeqComps(subsystem, count)
    subsystemMachines.get(subsystem).map(roundRobinOn(_, prefixes))
  }

  private def configToSeqComps(subsystem: Subsystem, noOfSeqComps: Int) =
    (1 to noOfSeqComps).map(i => Prefix(subsystem, s"${subsystem}_$i"))

  private def roundRobinOn(machines: List[AkkaLocation], prefixes: Seq[Prefix]) = prefixes.zip(cycle(machines: _*)).toMap

  private def cycle[T](elems: T*): LazyList[T] = LazyList(elems: _*) #::: cycle(elems: _*)
}
