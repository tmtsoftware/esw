package esw.ocs.dsl.script.utils

import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.BaseTestSuite

class SubsystemFactoryTest extends BaseTestSuite {
  "Subsystem" must {
    "be created with case-insensitive string  | ESW-279" in {
      SubsystemFactory.make("ESW") shouldBe ESW
      SubsystemFactory.make("esw") shouldBe ESW
    }
  }
}
