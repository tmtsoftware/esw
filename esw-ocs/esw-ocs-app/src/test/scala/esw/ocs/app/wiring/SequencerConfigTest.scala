package esw.ocs.app.wiring

import akka.Done
import com.typesafe.config.{Config, ConfigFactory}
import csw.params.commands.SequenceCommand
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import esw.http.core.BaseTestSuite
import esw.ocs.impl.core.script.ScriptLoadingException.ScriptConfigurationMissingException
import esw.ocs.impl.core.script.{ScriptApi, ScriptContext}

import scala.concurrent.Future

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config = ConfigFactory.load()

  "from" must {
    "create SequencerConfig based on subsystem and observingMode | ESW-103" in {
      val subsystem        = Subsystem.ESW
      val observingMode    = "darknight"
      val sequencerConfigs = SequencerConfig.from(config, subsystem, observingMode)

      sequencerConfigs.prefix.componentName should ===("darknight")
      sequencerConfigs.prefix should ===(Prefix(ESW, "darknight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given subsystem and observingMode | ESW-103" in {
      val subsystem     = Subsystem.CSW
      val observingMode = "invalidObservingMode"

      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(config, subsystem, observingMode)
      }
      exception.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [$observingMode]")
    }
  }
}

class ValidTestScript(ctx: ScriptContext) extends ScriptApi {
  override def execute(command: SequenceCommand): Future[Unit]                       = ???
  override def executeGoOnline(): Future[Done]                                       = ???
  override def executeGoOffline(): Future[Done]                                      = ???
  override def executeShutdown(): Future[Done]                                       = ???
  override def executeAbort(): Future[Done]                                          = ???
  override def executeStop(): Future[Done]                                           = ???
  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] = ???
  override def executeOperationsMode(): Future[Done]                                 = ???
}
