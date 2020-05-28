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

  private val ESW_SM                  = "esw-sm"
  private val SEQUENCER_START_RETRIES = "sequencerStartRetries"

  // Reads config file using csw config server
  def read(configFilePath: Path, isLocal: Boolean): Future[SequenceManagerConfig] =
    configUtils.getConfig(configFilePath, isLocal).map(parseConfig)

  private def parseConfig(config: Config): SequenceManagerConfig = {
    val configWithRetries = config.withFallback(ConfigFactory.load().withOnlyPath(s"$ESW_SM.$SEQUENCER_START_RETRIES"))
    val configStr         = configWithRetries.getConfig(ESW_SM).root().render(ConfigRenderOptions.concise())

    Json.decode(configStr.getBytes).to[SequenceManagerConfig].value
  }
}
