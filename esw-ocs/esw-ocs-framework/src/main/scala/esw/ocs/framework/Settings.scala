package esw.ocs.framework

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.core.models.Prefix

class Settings(config: Config = ConfigFactory.load()) {

  def sequencerSettings(sequencerId: String, observingMode: String): SequencerSettings = {
    val configPathForSequencer = s"scripts.$sequencerId.$observingMode"
    val sequencerName          = s"$sequencerId@$observingMode"
    val prefix                 = Prefix(config.getString(s"$configPathForSequencer.prefix"))
    val scriptClass            = config.getString(s"$configPathForSequencer.scriptClass")

    SequencerSettings(sequencerId, observingMode, sequencerName, prefix, scriptClass)
  }
}

case class SequencerSettings(
    sequencerId: String,
    observingMode: String,
    sequencerName: String,
    prefix: Prefix,
    scriptClass: String
)
