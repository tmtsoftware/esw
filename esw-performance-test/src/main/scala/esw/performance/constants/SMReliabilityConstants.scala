package esw.performance.constants

import csw.prefix.models.Prefix
import esw.sm.api.models.{AgentProvisionConfig, ProvisionConfig}

object SMReliabilityConstants {
  val provisionConfig: ProvisionConfig = ProvisionConfig(
    List(
      AgentProvisionConfig(Prefix("ESW.machine1"), 2),
      AgentProvisionConfig(Prefix("IRIS.machine1"), 1),
      AgentProvisionConfig(Prefix("TCS.machine1"), 1),
      AgentProvisionConfig(Prefix("AOESW.machine1"), 1),
      AgentProvisionConfig(Prefix("WFOS.machine1"), 1)
    )
  )
  val timeout: Int     = 5 * 1000
  val warmupIterations = 1
  val actualIterations = 330
}
