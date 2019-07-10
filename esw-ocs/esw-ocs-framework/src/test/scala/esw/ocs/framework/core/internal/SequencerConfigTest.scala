package esw.ocs.framework.core.internal

import csw.params.core.models.Prefix
import esw.ocs.framework.BaseTestSuite

class SequencerConfigTest extends BaseTestSuite {
  "SequencerConfig" must {
    "form name based on sequencerId and observingMode" in {
      val sequencerId      = "testSequencerId1"
      val observingMode    = "testObservingMode1"
      val sequencerConfigs = new SequencerConfig(sequencerId, observingMode)

      sequencerConfigs.name shouldEqual "testSequencerId1@testObservingMode1"
    }

    "form prefix based on sequencerId and observingMode" in {
      val sequencerId      = "testSequencerId1"
      val observingMode    = "testObservingMode1"
      val sequencerConfigs = new SequencerConfig(sequencerId, observingMode)

      sequencerConfigs.prefix shouldEqual Prefix("esw.ocs.prefix1")
    }

    "form scriptClass based on sequencerId and observingMode" in {
      val sequencerId      = "testSequencerId1"
      val observingMode    = "testObservingMode1"
      val sequencerConfigs = new SequencerConfig(sequencerId, observingMode)

      sequencerConfigs.scriptClass shouldEqual "esw.ocs.framework.core.internal.ValidTestScript"
    }
  }
}
