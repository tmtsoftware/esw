package esw.ocs.framework.core.internal

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.core.models.Prefix

class SequencerConfig(sequencerId: String, observingMode: String) {
  // fixme: load config only once in entire app, maybe in wiring and then pass around?
  private[internal] val config: Config         = ConfigFactory.load()
  private[internal] val configPathForSequencer = s"scripts.$sequencerId.$observingMode"
  val name                                     = s"$sequencerId@$observingMode"
  val prefix                                   = Prefix(config.getString(s"$configPathForSequencer.prefix"))
  val scriptClass: String                      = config.getString(s"$configPathForSequencer.scriptClass")
}
