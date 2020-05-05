package esw.sm.api.models

import csw.prefix.models.Subsystem.{ESW, NFIRAOS, TCS}
import esw.sm.api.BaseTestSuite

class SequenceManagerConfigTest extends BaseTestSuite {
  "Resources needed for observing mode" must {
    "create from strings" in {
      val resources = Resources("IRIS", "WFOS")

      resources.resources should ===(Set("IRIS", "WFOS"))
    }

    "check conflictsWith resources" in {
      val resources               = Resources("IRIS", "WFOS")
      val conflictingResources    = Resources("IRIS", "AOS")
      val nonConflictingResources = Resources("TCS", "NFIRAOS")

      resources.conflictsWith(conflictingResources) should ===(true)
      resources.conflictsWith(nonConflictingResources) should ===(false)
    }
  }

  "Sequencers needed for observing mode" must {
    "create from subsystems" in {
      val sequencers = Sequencers(ESW, TCS, NFIRAOS)

      sequencers.subsystems should ===(List(ESW, TCS, NFIRAOS))
    }
  }
}
