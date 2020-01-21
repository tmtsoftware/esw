package esw.ocs.app.wiring

import akka.Done
import com.typesafe.config.{Config, ConfigFactory}
import csw.params.commands.SequenceCommand
import csw.prefix.models.Subsystem.{NSCU, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import esw.http.core.BaseTestSuite
import esw.ocs.impl.script.ScriptLoadingException.ScriptConfigurationMissingException
import esw.ocs.impl.script.{ScriptApi, ScriptContext}

import scala.concurrent.Future

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config = ConfigFactory.load()

  "from" must {
    "create SequencerConfig based on subsystem and observingMode | ESW-103, ESW-279" in {
      val subsystem        = NSCU
      val observingMode    = "darknight"
      val sequencerConfigs = SequencerConfig.from(config, subsystem, observingMode)

      sequencerConfigs.prefix.componentName should ===("darknight")
      sequencerConfigs.prefix should ===(Prefix(NSCU, "darknight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
    }

    "create SequencerConfig based on case-sensitive subsystem and observingMode | ESW-103, ESW-279" in {
      val subsystem        = TCS
      val observingMode    = "DarkNight"
      val sequencerConfigs = SequencerConfig.from(config, subsystem, observingMode)

      sequencerConfigs.prefix.componentName should ===("DarkNight")
      sequencerConfigs.prefix should ===(Prefix(TCS, "DarkNight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)

      val lowerCaseObservingMode = "darknight"
      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(config, subsystem, lowerCaseObservingMode)
      }

      exception.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [$lowerCaseObservingMode]")

      val upperCaseObservingMode = "DARKNIGHT"
      val exception2 = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(config, subsystem, upperCaseObservingMode)
      }

      exception2.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [$upperCaseObservingMode]")
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
  override def executeExceptionHandlers(ex: Throwable): Future[Done]                 = ???
  override def shutdownScript(): Unit                                                = ???
}
