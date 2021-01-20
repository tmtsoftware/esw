package esw.agent.akka.app

import java.nio.file.Path

import com.typesafe.config.Config
import csw.prefix.models.Prefix

import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class AgentSettings(
    prefix: Prefix,
    durationToWaitForComponentRegistration: FiniteDuration,
    coursierChannel: String,
    versionConfPath: Path
)

object AgentSettings {
  def apply(prefix: Prefix, config: Config): AgentSettings = {
    val agentConfig = config.getConfig("agent")
    AgentSettings(
      prefix,
      agentConfig.getDuration("durationToWaitForComponentRegistration").toSeconds.seconds,
      agentConfig.getString("coursier.channel"),
      Path.of(agentConfig.getString("osw.version.confPath"))
    )
  }
}
