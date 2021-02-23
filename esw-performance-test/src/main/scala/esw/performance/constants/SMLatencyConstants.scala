package esw.performance.constants

import csw.prefix.models.Prefix
import esw.sm.api.models.{AgentProvisionConfig, ProvisionConfig}

object SMLatencyConstants {
  val enableSwitching = true

  val warmupIterations = 2
  val actualIterations = 20

  val timeout: Int = 60 * 1000
  val provisionConfig: ProvisionConfig = ProvisionConfig(
    List(
      AgentProvisionConfig(Prefix("ESW.machine1"), 1),
      AgentProvisionConfig(Prefix("IRIS.machine1"), 1),
      AgentProvisionConfig(Prefix("TCS.machine1"), 1),
      AgentProvisionConfig(Prefix("AOESW.machine1"), 1),
      AgentProvisionConfig(Prefix("WFOS.machine1"), 1)
    )
  )
}
