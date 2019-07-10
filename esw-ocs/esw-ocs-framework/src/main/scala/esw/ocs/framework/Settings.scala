package esw.ocs.framework

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.core.models.Prefix

class Settings(sequencerId: String, observingMode: String) {
  private lazy val config: Config    = ConfigFactory.load()
  private val configPathForSequencer = s"scripts.$sequencerId.$observingMode"
  val sequencerName                  = s"$sequencerId@$observingMode"
  val prefix                         = Prefix(config.getString(s"$configPathForSequencer.prefix"))
  val scriptClass: String            = config.getString(s"$configPathForSequencer.scriptClass")
}
