package esw.ocs.app.wiring

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.core.models.Prefix
import esw.http.core.BaseTestSuite
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptConfigurationMissingException
import esw.ocs.dsl.script.{CswServices, MainScriptDsl, StrandEc}

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config = ConfigFactory.load()

  "from" must {
    "create SequencerConfig based on packageId and observingMode | ESW-103" in {
      val packageId        = "esw"
      val observingMode    = "darknight"
      val sequencerConfigs = SequencerConfig.from(config, packageId, observingMode, None)

      sequencerConfigs.sequencerName should ===("esw@darknight")
      sequencerConfigs.prefix should ===(Prefix("esw.ocs.prefix1"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "create SequencerConfig based on packageId and observingMode | ESW-103, ESW-214" in {
      val packageId             = "esw"
      val observingMode         = "darknight"
      val sequenceComponentName = "ESW_1"
      val sequencerConfigs      = SequencerConfig.from(config, packageId, observingMode, Some(sequenceComponentName))

      sequencerConfigs.sequencerName should ===("ESW_1@esw@darknight")
      sequencerConfigs.prefix should ===(Prefix("esw.ocs.prefix1"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given packageId and observingMode | ESW-103" in {
      val packageId     = "invalidPackageId"
      val observingMode = "invalidObservingMode"

      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(config, packageId, observingMode, None)
      }
      exception.getMessage should ===(s"Script configuration missing for $packageId with $observingMode")
    }
  }
}

class ValidTestScript(csw: CswServices) extends MainScriptDsl(csw, StrandEc())
