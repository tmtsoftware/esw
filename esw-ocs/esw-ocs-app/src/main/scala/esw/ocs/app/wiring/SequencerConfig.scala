package esw.ocs.app.wiring

import java.time.Duration

import com.typesafe.config.{Config, ConfigException}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.ocs.impl.script.ScriptLoadingException.ScriptConfigurationMissingException

private[ocs] final case class SequencerConfig(
    prefix: Prefix,
    scriptClass: String,
    heartbeatInterval: Duration,
    enableThreadMonitoring: Boolean
)

private[ocs] object SequencerConfig {
  def from(config: Config, subsystem: Subsystem, observingMode: ObsMode): SequencerConfig = {
    val scriptConfig =
      try {
        config.getConfig(s"scripts.${subsystem.name}.${observingMode.name}")
      }
      catch {
        case _: ConfigException.Missing => throw new ScriptConfigurationMissingException(subsystem, observingMode)
      }

    val scriptClass            = scriptConfig.getString("scriptClass")
    val heartbeatInterval      = config.getDuration("esw.heartbeat-interval")
    val enableThreadMonitoring = config.getBoolean("esw.enable-thread-monitoring")
    SequencerConfig(Prefix(s"$subsystem.${observingMode.name}"), scriptClass, heartbeatInterval, enableThreadMonitoring)
  }
}
