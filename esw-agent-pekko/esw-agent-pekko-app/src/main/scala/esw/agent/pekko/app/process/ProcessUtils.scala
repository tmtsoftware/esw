package esw.agent.pekko.app.process

/**
 * This object allows verification of native CLI applications whether they are installed or not. It prevents unnecessary `command not found` error.
 */
object ProcessUtils {
  def isInstalled(cmd: String): Boolean = new ProcessBuilder("bash", "-c", s"command -v $cmd").start().waitFor() == 0
}
