package esw.agent.app

import java.nio.file.{Path, Paths}

import com.typesafe.config.Config

import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class AgentSettings(
    private val _binariesPath: String,
    durationToWaitForComponentRegistration: FiniteDuration
) {
  private val path = Paths.get(_binariesPath)

  val binariesPath: Path = path match {
    case _ if path.isAbsolute => path
    case _ if _binariesPath.startsWith("~") =>
      Paths.get(_binariesPath.replaceFirst("~", System.getProperty("user.home"))) //path relative to home dir
    case _ => throw new RuntimeException("binariesPath should be absolute path") //path relative to current dir
  }
}

object AgentSettings {
  def from(agentConfig: Config): AgentSettings = AgentSettings(
    agentConfig.getString("binariesPath"),
    agentConfig.getDuration("durationToWaitForComponentRegistration").toNanos.nanos
  )
}
