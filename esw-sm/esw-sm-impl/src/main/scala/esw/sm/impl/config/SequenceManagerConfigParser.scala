package esw.sm.impl.config

import java.nio.file.Path

import com.typesafe.config.ConfigRenderOptions
import csw.config.client.commons.ConfigUtils
import io.bullet.borer._

import scala.concurrent.{ExecutionContext, Future}

// Reads config file for all observing modes and parse it into SequenceManagerConfig
//(Map observing mode to Resources and Sequencers)
class SequenceManagerConfigParser(configUtils: ConfigUtils)(implicit ec: ExecutionContext) {
  import ConfigCodecs._
  private val EswSmKey           = "esw-sm"
  private val ObsModesKey        = "obsModes"
  private val ProvisionConfigKey = "provision"

  def readObsModeConfig(configFilePath: Path, isLocal: Option[Boolean]): Future[SequenceManagerConfig] =
    readConfig[SequenceManagerConfig](ObsModesKey, configFilePath, isLocal)

  def readProvisionConfig(configFilePath: Path, isLocal: Option[Boolean]): Future[ProvisionConfig] =
    readConfig[ProvisionConfig](ProvisionConfigKey, configFilePath, isLocal)

  // Reads config file from config service or local filesystem
  private def readConfig[T: Decoder](key: String, configFilePath: Path, isLocal: Option[Boolean]): Future[T] = {
    configUtils.getConfig(configFilePath, isLocal.getOrElse(true)).map { config =>
      val configStr = config.getConfig(s"$EswSmKey.$key").root().render(ConfigRenderOptions.concise())
      Json.decode(configStr.getBytes).to[T].value
    }
  }
}
