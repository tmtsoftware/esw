package esw.sm.impl.core

import java.nio.file.Path

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.config.client.commons.ConfigUtils
import esw.sm.api.codecs.SequenceManagerCodecs
import esw.sm.api.models.SequenceManagerConfig
import io.bullet.borer._

import scala.concurrent.{ExecutionContext, Future}

// Reads config file for all observing modes and parse it into SequenceManagerConfig
//(Map observing mode to Resources and Sequencers)
class SequenceManagerConfigParser(configUtils: ConfigUtils)(implicit ec: ExecutionContext) extends SequenceManagerCodecs {
  private val ObsModesKey = "obsModes"

  // Reads config file using csw config server
  def read(configFilePath: Path, isLocal: Boolean): Future[SequenceManagerConfig] =
    configUtils.getConfig(configFilePath, isLocal).map(parseConfig)

  private def parseConfig(config: Config): SequenceManagerConfig = {
    val configStr = config.getConfig(ObsModesKey).root().render(ConfigRenderOptions.concise())

    Json.decode(configStr.getBytes).to[SequenceManagerConfig].value
  }
}
