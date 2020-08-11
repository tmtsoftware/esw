package esw.agent.akka.app

import com.typesafe.config.Config

import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class AgentSettings(durationToWaitForComponentRegistration: FiniteDuration, coursierChannel: String)

object AgentSettings {
  def from(config: Config): AgentSettings = {
    val agentConfig = config.getConfig("agent")
    AgentSettings(
      agentConfig.getDuration("durationToWaitForComponentRegistration").toSeconds.seconds,
      agentConfig.getString("coursier.channel")
    )
  }
}
