package esw.agent.akka.app

import java.nio.file.Path

import com.typesafe.config.Config
import csw.prefix.models.Prefix

case class AgentSettings(
    prefix: Prefix,
    coursierChannel: String,
    versionConfPath: Path,
    gcMetricsEnabled: Boolean
)

object AgentSettings {
  def apply(prefix: Prefix, config: Config): AgentSettings = {
    val agentConfig = config.getConfig("agent")
    AgentSettings(
      prefix,
      agentConfig.getString("coursier.channel"),
      Path.of(agentConfig.getString("osw.version.confPath")),
      agentConfig.getBoolean("enable-gc-metrics")
    )
  }
}
