package esw.sm.impl.utils

import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent

class SequenceComponentUtilIntegrationTest extends EswTestKit(MachineAgent) {
  "getAvailableSequenceComponent" must {
    "spawn sequence component if subsystem and ESW fallback sequence component is not available | ESW-164" in {
      val locationServiceUtil   = new LocationServiceUtil(locationService)
      val agentUtil             = new AgentUtil(locationServiceUtil)
      val sequenceComponentUtil = new SequenceComponentUtil(locationServiceUtil, agentUtil)

      // ESW-164 verify SequenceComponentUtil can successfully spawn sequence component
      sequenceComponentUtil.getAvailableSequenceComponent(ESW).rightValue shouldBe a[SequenceComponentApi]
    }
  }
}
