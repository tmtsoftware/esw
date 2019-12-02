package esw.ocs.app.wiring

import com.typesafe.config.{Config, ConfigException}
import csw.params.core.models.Prefix
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptConfigurationMissingException

private[app] final case class SequencerConfig(prefix: Prefix, scriptClass: String)

private[app] object SequencerConfig {
  def from(config: Config, packageId: String, observingMode: String, sequenceComponentName: Option[String]): SequencerConfig = {
    val scriptConfig =
      try {
        config.getConfig(s"scripts.$packageId.$observingMode")
      } catch {
        case _: ConfigException.Missing => throw new ScriptConfigurationMissingException(packageId, observingMode)
      }

    val sequencerName = sequenceComponentName match {
      case Some(name) => s"$name@$packageId@$observingMode"
      case None       => s"$packageId@$observingMode"
    }

    val scriptClass = scriptConfig.getString("scriptClass")

    SequencerConfig(Prefix(s"$packageId.$sequencerName"), scriptClass)
  }
}
