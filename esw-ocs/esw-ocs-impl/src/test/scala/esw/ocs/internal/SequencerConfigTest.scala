package esw.ocs.internal

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.core.models.Prefix
import esw.ocs.BaseTestSuite
import esw.ocs.exceptions.ScriptLoadingException.ScriptConfigurationMissingException

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config = ConfigFactory.load()

  "from" must {
    "create SequencerConfig based on sequencerId and observingMode | ESW-103" in {
      val sequencerId      = "testSequencerId1"
      val observingMode    = "testObservingMode1"
      val sequencerConfigs = SequencerConfig.from(config, sequencerId, observingMode)

      sequencerConfigs.sequencerName should ===("testSequencerId1@testObservingMode1")
      sequencerConfigs.prefix should ===(Prefix("esw.ocs.prefix1"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given sequencerId and observingMode | ESW-103" in {
      val sequencerId   = "invalidSequencerId"
      val observingMode = "invalidObservingMode"

      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(config, sequencerId, observingMode)
      }
      exception.getMessage should ===(s"Script configuration missing for $sequencerId with $observingMode")
    }
  }
}
