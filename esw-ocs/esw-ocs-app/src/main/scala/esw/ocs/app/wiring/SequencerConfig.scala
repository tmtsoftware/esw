package esw.ocs.app.wiring

import com.typesafe.config.{Config, ConfigException}
import csw.params.core.models.Prefix
import esw.dsl.script.exceptions.ScriptLoadingException.ScriptConfigurationMissingException

private[app] final case class SequencerConfig(sequencerName: String, prefix: Prefix, scriptClass: String)

private[app] object SequencerConfig {
  def from(config: Config, sequencerId: String, observingMode: String, sequenceComponentName: Option[String]): SequencerConfig = {
    val scriptConfig =
      try {
        config.getConfig(s"scripts.$sequencerId.$observingMode")
      } catch {
        case _: ConfigException.Missing => throw new ScriptConfigurationMissingException(sequencerId, observingMode)
      }

    val sequencerName = sequenceComponentName match {
      case Some(name) => s"$name@$sequencerId@$observingMode"
      case None       => s"$sequencerId@$observingMode"
    }

    val prefix      = scriptConfig.getString("prefix")
    val scriptClass = scriptConfig.getString("scriptClass")

    SequencerConfig(sequencerName, Prefix(prefix), scriptClass)
  }
}
