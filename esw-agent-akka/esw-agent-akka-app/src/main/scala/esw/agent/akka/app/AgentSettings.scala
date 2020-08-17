package esw.agent.akka.app

import com.typesafe.config.Config
import csw.prefix.models.Prefix

import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class AgentSettings(prefix: Prefix, durationToWaitForComponentRegistration: FiniteDuration, coursierChannel: String)

object AgentSettings {
  def apply(prefix: Prefix, config: Config): AgentSettings = {
    val agentConfig = config.getConfig("agent")
    AgentSettings(
      prefix,
      agentConfig.getDuration("durationToWaitForComponentRegistration").toSeconds.seconds,
      agentConfig.getString("coursier.channel")
    )
  }
}
