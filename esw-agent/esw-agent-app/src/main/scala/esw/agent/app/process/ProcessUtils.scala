package esw.agent.app.process

object ProcessUtils {
  def isInstalled(cmd: String): Boolean = new ProcessBuilder("bash", "-c", "command", "-v", cmd).start().waitFor() == 0
}
