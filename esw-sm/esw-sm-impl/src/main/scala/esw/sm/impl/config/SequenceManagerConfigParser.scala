package esw.sm.impl.config

import java.nio.file.Path

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.config.client.commons.ConfigUtils
import io.bullet.borer._

import scala.concurrent.{ExecutionContext, Future}

// Reads config file for all observing modes and parse it into SequenceManagerConfig
//(Map observing mode to Resources and Sequencers)
class SequenceManagerConfigParser(configUtils: ConfigUtils)(implicit ec: ExecutionContext) {
  import ConfigCodecs._

  private val EswSmKey = "esw-sm"
  private val ObsModes = "obsModes"

  // Reads config file from config service or local filesystem
  def read(configFilePath: Path, isLocal: Boolean): Future[SequenceManagerConfig] =
    configUtils.getConfig(configFilePath, isLocal).map(parseConfig)

  private def parseConfig(config: Config): SequenceManagerConfig = {
    val configStr = config.getConfig(s"$EswSmKey.$ObsModes").root().render(ConfigRenderOptions.concise())
    Json.decode(configStr.getBytes).to[SequenceManagerConfig].value
  }
}
