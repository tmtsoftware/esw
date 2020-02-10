package esw.ocs.app.wiring

import java.time.Duration

import com.typesafe.config.{Config, ConfigException}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.impl.script.ScriptLoadingException.ScriptConfigurationMissingException

private[app] final case class SequencerConfig(prefix: Prefix, scriptClass: String, heartbeatInterval: Duration)

private[app] object SequencerConfig {
  def from(config: Config, subsystem: Subsystem, observingMode: String): SequencerConfig = {
    val scriptConfig =
      try {
        config.getConfig(s"scripts.${subsystem.name}.$observingMode")
      }
      catch {
        case _: ConfigException.Missing => throw new ScriptConfigurationMissingException(subsystem, observingMode)
      }

    val scriptClass       = scriptConfig.getString("scriptClass")
    val heartbeatInterval = config.getDuration("esw.heartbeat-interval")
    SequencerConfig(Prefix(s"$subsystem.$observingMode"), scriptClass, heartbeatInterval)
  }
}
