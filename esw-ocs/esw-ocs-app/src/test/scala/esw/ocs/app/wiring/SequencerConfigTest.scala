package esw.ocs.app.wiring

import akka.Done
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import csw.params.commands.SequenceCommand
import csw.prefix.models.Subsystem.{APS, IRIS, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import esw.ocs.api.models.ObsMode
import esw.ocs.impl.script.exceptions.ScriptLoadingException.ScriptConfigurationMissingException
import esw.ocs.impl.script.{ScriptApi, ScriptContext}
import esw.testcommons.BaseTestSuite

import java.time.Duration
import scala.concurrent.Future

class SequencerConfigTest extends BaseTestSuite {
  private val config: Config      = ConfigFactory.load()
  private val validConf: Config   = config.getConfig("valid-conf")
  private val invalidConf: Config = config.getConfig("invalid-conf")

  "from" must {
    "create SequencerConfig based on subsystem and sequencer Id | ESW-103, ESW-279, ESW-290, ESW-561" in {
      val subsystem        = APS
      val obsMode          = ObsMode("darknight")
      val sequencerConfigs = SequencerConfig.from(validConf, Prefix(subsystem, obsMode.name))

      sequencerConfigs.prefix.componentName should ===("darknight")
      sequencerConfigs.prefix should ===(Prefix(APS, "darknight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
      sequencerConfigs.heartbeatInterval should ===(Duration.ofSeconds(3))
      sequencerConfigs.enableThreadMonitoring should ===(true)
    }

    "create SequencerConfig based on subsystem and sequencer Id with variation | ESW-561" in {
      val subsystem        = IRIS
      val sequencerConfigs = SequencerConfig.from(validConf, Prefix(subsystem, "IRIS_ImagerAndIFS.IRIS_IMAGER"))

      sequencerConfigs.prefix.componentName should ===("IRIS_ImagerAndIFS.IRIS_IMAGER")
      sequencerConfigs.prefix should ===(Prefix(subsystem, "IRIS_ImagerAndIFS.IRIS_IMAGER"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)
      sequencerConfigs.heartbeatInterval should ===(Duration.ofSeconds(3))
      sequencerConfigs.enableThreadMonitoring should ===(true)
    }

    "create SequencerConfig based on case-sensitive subsystem and sequencer Id | ESW-103, ESW-279, ESW-561" in {
      val subsystem        = TCS
      val obsMode          = ObsMode("DarkNight")
      val sequencerConfigs = SequencerConfig.from(validConf, Prefix(subsystem, obsMode.name))

      sequencerConfigs.prefix.componentName should ===("DarkNight")
      sequencerConfigs.prefix should ===(Prefix(TCS, "DarkNight"))
      sequencerConfigs.scriptClass should ===(classOf[ValidTestScript].getCanonicalName)

      val lowerCaseObsMode = obsMode.name.toLowerCase
      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(validConf, Prefix(subsystem, lowerCaseObsMode))
      }

      exception.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [$lowerCaseObsMode]")

      val upperCaseObsMode = obsMode.name.toUpperCase
      val exception2 = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(validConf, Prefix(subsystem, upperCaseObsMode))
      }

      exception2.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [$upperCaseObsMode]")
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given subsystem and sequencer Id | ESW-103, ESW-561" in {
      val subsystem = Subsystem.CSW
      val obsMode   = ObsMode("invalidObsMode")

      val exception = intercept[ScriptConfigurationMissingException] {
        SequencerConfig.from(validConf, Prefix(subsystem, obsMode.name))
      }
      exception.getMessage should ===(s"Script configuration missing for [${subsystem.name}] with [${obsMode.name}]")
    }

    "throw Exception if heartbeat-interval is missing | ESW-290" in {
      val subsystem = TCS
      val obsMode   = ObsMode("DarkNight")

      intercept[ConfigException.Missing] {
        SequencerConfig.from(invalidConf, Prefix(subsystem, obsMode.name))
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
