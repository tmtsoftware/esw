package esw.ocs.app.wiring

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.core.models.Prefix
import esw.http.core.BaseTestSuite
import esw.dsl.script.exceptions.ScriptLoadingException.ScriptConfigurationMissingException
import esw.ocs.impl.internal.ValidTestScript

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config = ConfigFactory.load()

  "from" must {
    "create SequencerConfig based on sequencerId and observingMode | ESW-103" in {
      val sequencerId      = "testSequencerId1"
      val observingMode    = "testObservingMode1"
      val sequencerConfigs = SequencerConfig.from(config, sequencerId, observingMode, None)

      sequencerConfigs.sequencerName should ===("testSequencerId1@testObservingMode1")
      sequencerConfigs.prefix should ===(Prefix("esw.ocs.prefix1"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "create SequencerConfig based on sequencerId and observingMode | ESW-103, ESW-214" in {
      val sequencerId           = "testSequencerId1"
      val observingMode         = "testObservingMode1"
      val sequenceComponentName = "OCS_1"
      val sequencerConfigs      = SequencerConfig.from(config, sequencerId, observingMode, Some(sequenceComponentName))

      sequencerConfigs.sequencerName should ===("OCS_1@testSequencerId1@testObservingMode1")
      sequencerConfigs.prefix should ===(Prefix("esw.ocs.prefix1"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given sequencerId and observingMode | ESW-103" in {
      val sequencerId   = "invalidSequencerId"
      val observingMode = "invalidObservingMode"

      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(config, sequencerId, observingMode, None)
      }
      exception.getMessage should ===(s"Script configuration missing for $sequencerId with $observingMode")
    }
  }
}
