package esw.ocs.internal

import com.typesafe.config.{Config, ConfigException}
import csw.params.core.models.Prefix
import esw.ocs.exceptions.ScriptLoadingException.ScriptConfigurationMissingException

private[internal] final case class SequencerConfig(sequencerName: String, prefix: Prefix, scriptClass: String)

private[internal] object SequencerConfig {
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
