package esw.ocs.app.wiring

import com.typesafe.config.{Config, ConfigException}
import csw.prefix.models.Prefix
import esw.ocs.impl.script.ScriptLoadingException.ScriptConfigurationMissingException

import java.time.Duration

/**
 * This is the sequencer config. which includes
 *
 * @param prefix - prefix of the sequencer
 * @param scriptClass - the classpath of the script which is to be loaded in the sequencer
 * @param heartbeatInterval -  interval to health check
 * @param enableThreadMonitoring - boolean param to enable the thread monitoring
 */
final case class SequencerConfig(
    prefix: Prefix,
    scriptClass: String,
    heartbeatInterval: Duration,
    enableThreadMonitoring: Boolean
)

object SequencerConfig {
  def from(config: Config, prefix: Prefix): SequencerConfig = {
    val scriptConfig =
      try {
        config.getConfig(s"scripts.$prefix")
      }
      catch {
        case _: ConfigException.Missing => throw new ScriptConfigurationMissingException(prefix.subsystem, prefix.componentName)
      }

    val scriptClass            = scriptConfig.getString("scriptClass")
    val heartbeatInterval      = config.getDuration("esw.heartbeat-interval")
    val enableThreadMonitoring = config.getBoolean("esw.enable-thread-monitoring")
    SequencerConfig(prefix, scriptClass, heartbeatInterval, enableThreadMonitoring)
  }
}
