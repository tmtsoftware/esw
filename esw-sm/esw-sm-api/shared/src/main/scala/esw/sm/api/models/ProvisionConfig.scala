/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.api.models

import csw.prefix.models.Prefix
import esw.sm.api.models.ProvisionConfig.{AgentPrefix, SequenceComponentPrefix}

/**
 * This model class represents mapping of agent and how many sequence components will be running on the agen
 *
 * @param agentPrefix - prefix of agent
 * @param countOfSeqComps - number of sequence components
 */
case class AgentProvisionConfig(agentPrefix: Prefix, countOfSeqComps: Int) {
  // check count is greater than Zero
  require(
    countOfSeqComps >= 0,
    "Invalid sequence component count: Count of sequence components must be greater than or equal to Zero"
  )
}

/**
 * This model class has the list of AgentProvisionConfig fro all the agents
 *
 * @param config - list of AgentProvisionConfig
 */
case class ProvisionConfig(config: List[AgentProvisionConfig]) {
  private val repeatedPrefix: List[AgentProvisionConfig] = config.diff(config.distinctBy(_.agentPrefix))
  // Check there are no double entries of any prefix
  require(
    repeatedPrefix.isEmpty,
    s"Invalid Provision config: Cannot have multiple entries for same agent prefix. :$repeatedPrefix"
  )

  /**
   * Returns the list of mappings of which sequence component will be running on which agent
   *
   * @return mapping of AgentPrefix with SequenceComponentPrefix
   */
  def agentToSeqCompMapping: List[(AgentPrefix, SequenceComponentPrefix)] = {
    def assign(config: List[AgentProvisionConfig], from: Int): List[(AgentPrefix, SequenceComponentPrefix)] =
      config match {
        case Nil => List.empty
        case AgentProvisionConfig(agentPrefix, count) :: rest =>
          generateSeqCompPrefix(agentPrefix, count, from) ++ assign(rest, from + count)
      }

    groupPrefixBySubsystem.flatMap(assign(_, 1)).toList
  }

  private def groupPrefixBySubsystem = config.groupBy(_.agentPrefix.subsystem).values

  // Sequence component prefix are generated using subsystem of agent machine on which sequence component is spawned. Component name
  // for sequence component is generated as a combination of subsysem_<unique integer identifier> ex: ESW.ESW_3
  private def generateSeqCompPrefix(agentPrefix: AgentPrefix, count: Int, from: Int) = {
    (0 until count).map(i => (agentPrefix, Prefix(agentPrefix.subsystem, s"${agentPrefix.subsystem}_${from + i}"))).toList
  }
}

object ProvisionConfig {
  type AgentPrefix             = Prefix
  type SequenceComponentPrefix = Prefix

  def apply(config: (Prefix, Int)*): ProvisionConfig = ProvisionConfig(config.map(x => AgentProvisionConfig(x._1, x._2)).toList)
}
