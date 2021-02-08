package esw.sm.impl.config

import com.typesafe.config.ConfigRenderOptions
import csw.config.client.commons.ConfigUtils
import io.bullet.borer._

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}

// Reads config file for all observing modes and parse it into SequenceManagerConfig
//(Map observing mode to Resources and Sequencers)
class SequenceManagerConfigParser(configUtils: ConfigUtils)(implicit ec: ExecutionContext) {
  import ConfigCodecs._
  private val EswSmKey    = "esw-sm"
  private val ObsModesKey = "obsModes"

  def read(configFilePath: Path, isLocal: Boolean): Future[SequenceManagerConfig] =
    readConfig[SequenceManagerConfig](ObsModesKey, configFilePath, isLocal)

  // Reads config file from config service or local filesystem
  private def readConfig[T: Decoder](key: String, configFilePath: Path, isLocal: Boolean): Future[T] = {
    configUtils.getConfig(configFilePath, isLocal).map { config =>
      val configStr = config.getConfig(s"$EswSmKey.$key").root().render(ConfigRenderOptions.concise())
      Json.decode(configStr.getBytes).to[T].value
    }
  }
}
