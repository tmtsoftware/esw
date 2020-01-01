package agent

import java.nio.file.{Path, Paths}

import scala.concurrent.duration.FiniteDuration

case class AgentSettings(
    private val _binariesPath: String,
    durationToWaitForComponentRegistration: FiniteDuration
) {
  private val path = Paths.get(_binariesPath)
  def binariesPath: Path =
    if (path.isAbsolute) path
    else {
      //path relative to home dir
      if (_binariesPath.startsWith("~"))
        Paths.get(_binariesPath.replaceFirst("~", System.getProperty("user.home")))
      //path relative to current dir
      else throw new RuntimeException("binariesPath should be absolute path")
    }
}
