package esw.ocs.app.wiring

import java.time.Duration

import akka.Done
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import csw.params.commands.SequenceCommand
import csw.prefix.models.Subsystem.{NSCU, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import esw.ocs.api.models.ObsMode
import esw.ocs.impl.script.ScriptLoadingException.ScriptConfigurationMissingException
import esw.ocs.impl.script.{ScriptApi, ScriptContext}
import esw.testcommons.BaseTestSuite

import scala.concurrent.Future

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config      = ConfigFactory.load()
  private val validConf: Config   = config.getConfig("valid-conf")
  private val invalidConf: Config = config.getConfig("invalid-conf")

  "from" must {
    "create SequencerConfig based on subsystem and obsMode | ESW-103, ESW-279, ESW-290" in {
      val subsystem        = NSCU
      val obsMode          = ObsMode("darknight")
      val sequencerConfigs = SequencerConfig.from(validConf, subsystem, obsMode)

      sequencerConfigs.prefix.componentName should ===("darknight")
      sequencerConfigs.prefix should ===(Prefix(NSCU, "darknight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
      sequencerConfigs.heartbeatInterval should ===(Duration.ofSeconds(3))
      sequencerConfigs.enableThreadMonitoring should ===(true)
    }

    "create SequencerConfig based on case-sensitive subsystem and obsMode | ESW-103, ESW-279" in {
      val subsystem        = TCS
      val obsMode          = ObsMode("DarkNight")
      val sequencerConfigs = SequencerConfig.from(validConf, subsystem, obsMode)

      sequencerConfigs.prefix.componentName should ===("DarkNight")
      sequencerConfigs.prefix should ===(Prefix(TCS, "DarkNight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)

      val lowerCaseObsMode = obsMode.name.toLowerCase
      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(validConf, subsystem, ObsMode(lowerCaseObsMode))
      }

      exception.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [$lowerCaseObsMode]")

      val upperCaseObsMode = obsMode.name.toUpperCase
      val exception2 = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(validConf, subsystem, ObsMode(upperCaseObsMode))
      }

      exception2.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [$upperCaseObsMode]")
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given subsystem and obsMode | ESW-103" in {
      val subsystem = Subsystem.CSW
      val obsMode   = ObsMode("invalidObsMode")

      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(validConf, subsystem, obsMode)
      }
      exception.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [${obsMode.name}]")
    }

    "throw Exception if heartbeat-interval is missing | ESW-290" in {
      val subsystem = TCS
      val obsMode   = ObsMode("DarkNight")

      intercept[ConfigException.Missing] {
        SequencerConfig.from(invalidConf, subsystem, obsMode)
      }
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
  override def executeNewSequenceHandler(): Future[Done]                             = ???
}
