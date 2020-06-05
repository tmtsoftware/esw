package esw.agent.app.process

import scala.util.Try

object ProcessUtils {
  // new ProcessBuilder("command", "-v", cmd).start().waitFor() == 0 does not work from docker container
  def isInstalled(cmd: String*): Boolean = Try { new ProcessBuilder(cmd: _*).start().waitFor() }.isSuccess
}
