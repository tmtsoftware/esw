package esw.sm.impl.core

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.config.client.commons.ConfigUtils
import esw.sm.api.codecs.SequenceManagerCodecs
import esw.sm.api.models.SequenceManagerConfig
import io.bullet.borer._

import scala.concurrent.Future

class SequenceManagerConfigParser(configUtils: ConfigUtils)(implicit val actorSystem: ActorSystem[_])
    extends SequenceManagerCodecs {
  import actorSystem.executionContext

  private val OBSERVATION_MODES = "obsModes"

  def read(isLocal: Boolean, configFilePath: Option[Path], defaultConfig: Option[Config]): Future[SequenceManagerConfig] = {
    configUtils.getConfig(isLocal, configFilePath, defaultConfig).map(config => parseConfig(config))
  }

  private def parseConfig(config: Config): SequenceManagerConfig = {
    val str = config.getConfig(OBSERVATION_MODES).root().render(ConfigRenderOptions.concise())

    Json.decode(str.getBytes).to[SequenceManagerConfig].value
  }
}
