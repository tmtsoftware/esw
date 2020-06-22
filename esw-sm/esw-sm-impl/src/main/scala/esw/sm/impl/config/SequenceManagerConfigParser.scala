package esw.sm.impl.config

import java.nio.file.Path

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.config.client.commons.ConfigUtils
import io.bullet.borer._

import scala.concurrent.{ExecutionContext, Future}

// Reads config file for all observing modes and parse it into SequenceManagerConfig
//(Map observing mode to Resources and Sequencers)
class SequenceManagerConfigParser(configUtils: ConfigUtils)(implicit ec: ExecutionContext) {
  import ConfigCodecs._

  private val EswSmKey                 = "esw-sm"
  private val SequencerStartRetriesKey = "sequencerStartRetries"

  // Reads config file from config service or local filesystem
  def read(configFilePath: Path, isLocal: Boolean): Future[SequenceManagerConfig] =
    configUtils.getConfig(configFilePath, isLocal).map(parseConfig)

  private def parseConfig(config: Config): SequenceManagerConfig = {
    // pick retries from provided config. If not present then pick from application.conf of esw-sm-app as fallback
    val configWithRetries = config.withFallback(ConfigFactory.load().withOnlyPath(s"$EswSmKey.$SequencerStartRetriesKey"))
    val configStr         = configWithRetries.getConfig(EswSmKey).root().render(ConfigRenderOptions.concise())

    Json.decode(configStr.getBytes).to[SequenceManagerConfig].value
  }
}
