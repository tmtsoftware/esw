package esw.sm.api.models

import csw.prefix.models.Prefix
import esw.sm.api.models.ProvisionConfig.{AgentPrefix, SequenceComponentPrefix}

case class ProvisionConfig(config: Map[AgentPrefix, Int]) {
  require(config.forall(_._2 > 0), "Invalid Provision config: Count of sequence components cannot be Zero or less than Zero")

  def agentToSeqCompMapping: List[(AgentPrefix, SequenceComponentPrefix)] = {
    def assign(config: List[(AgentPrefix, Int)], from: Int): List[(AgentPrefix, SequenceComponentPrefix)] =
      config match {
        case Nil                          => List.empty
        case (agentPrefix, count) :: rest => generateSeqCompPrefix(agentPrefix, count, from) ++ assign(rest, from + count)
      }

    groupPrefixBySubsystem.flatMap(assign(_, 1)).toList
  }

  private def groupPrefixBySubsystem = config.groupBy(_._1.subsystem).view.mapValues(_.toList).values

  private def generateSeqCompPrefix(agentPrefix: AgentPrefix, count: Int, from: Int) =
    List.fill(count)(agentPrefix).zipWithIndex.map {
      case (_, i) => (agentPrefix, Prefix(agentPrefix.subsystem, s"${agentPrefix.subsystem}_${from + i}"))
    }
}

object ProvisionConfig {
  type AgentPrefix             = Prefix
  type SequenceComponentPrefix = Prefix
}
