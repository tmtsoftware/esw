package esw.sm.api.models

import csw.prefix.models.Subsystem.{ESW, NFIRAOS, TCS}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SequenceManagerConfigTest extends AnyWordSpecLike with Matchers with TypeCheckedTripleEquals {
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
