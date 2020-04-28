package esw.sm.impl.core

import java.nio.file.Path

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.config.client.commons.ConfigUtils
import esw.sm.api.codecs.SequenceManagerCodecs
import esw.sm.api.models.SequenceManagerConfig
import io.bullet.borer._

import scala.concurrent.{ExecutionContext, Future}

class SequenceManagerConfigParser(configUtils: ConfigUtils)(implicit ec: ExecutionContext) extends SequenceManagerCodecs {
  private val ObsModesKey = "obsModes"

  def read(configFilePath: Path, isLocal: Boolean): Future[SequenceManagerConfig] =
    configUtils.getConfig(configFilePath, isLocal).map(parseConfig)

  private def parseConfig(config: Config): SequenceManagerConfig = {
    val str = config.getConfig(ObsModesKey).root().render(ConfigRenderOptions.concise())

    Json.decode(str.getBytes).to[SequenceManagerConfig].value
  }
}
