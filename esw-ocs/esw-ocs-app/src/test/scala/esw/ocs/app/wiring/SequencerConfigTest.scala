package esw.ocs.app.wiring

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.core.models.Prefix
import esw.http.core.BaseTestSuite
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptConfigurationMissingException
import esw.ocs.dsl.script.{CswServices, ScriptDsl, StrandEc}

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config = ConfigFactory.load()

  "from" must {
    "create SequencerConfig based on packageId and observingMode | ESW-103" in {
      val packageId        = "esw"
      val observingMode    = "darknight"
      val sequencerConfigs = SequencerConfig.from(config, packageId, observingMode)

      sequencerConfigs.prefix.componentName should ===("darknight")
      sequencerConfigs.prefix should ===(Prefix("esw.darknight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given packageId and observingMode | ESW-103" in {
      val packageId     = "invalidPackageId"
      val observingMode = "invalidObservingMode"

      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(config, packageId, observingMode)
      }
      exception.getMessage should ===(s"Script configuration missing for $packageId with $observingMode")
    }
  }
}

class ValidTestScript(csw: CswServices) extends ScriptDsl(csw, StrandEc())
