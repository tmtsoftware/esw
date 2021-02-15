package esw.performance

import csw.prefix.models.Prefix
import esw.ocs.api.models.ObsMode
import esw.sm.api.models.{AgentProvisionConfig, ProvisionConfig}

object Constants {

  val obsMode1: ObsMode = ObsMode("obsMode1")
  val obsMode2: ObsMode = ObsMode("obsMode2")
  val obsMode3: ObsMode = ObsMode("obsMode3")
  val obsMode4: ObsMode = ObsMode("obsMode4")

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

  val provisionConfigReliability: ProvisionConfig = ProvisionConfig(
    List(
      AgentProvisionConfig(Prefix("ESW.machine1"), 2),
      AgentProvisionConfig(Prefix("IRIS.machine1"), 1),
      AgentProvisionConfig(Prefix("TCS.machine1"), 1),
      AgentProvisionConfig(Prefix("AOESW.machine1"), 1),
      AgentProvisionConfig(Prefix("WFOS.machine1"), 1)
    )
  )

  val warmupIterationsOverhead = 100
  val actualIterationsOverhead = 2000

  val warmupIterationsReliability = 1
  val actualIterationsReliability = 30
}
