package esw.sm.impl.utils

import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent

class AgentUtilIntegrationTest extends EswTestKit(MachineAgent) {
  "spawnSequenceComponentFor" must {
    "spawn sequence component | ESW-164" in {
      val locationServiceUtil = new LocationServiceUtil(locationService)
      val agentUtil           = new AgentUtil(locationServiceUtil)

      // ESW-164 verify that agent util can successfully spawn sequence component
      agentUtil
        .spawnSequenceComponentFor(ESW)
        .rightValue shouldBe a[SequenceComponentApi]
    }
  }
}
