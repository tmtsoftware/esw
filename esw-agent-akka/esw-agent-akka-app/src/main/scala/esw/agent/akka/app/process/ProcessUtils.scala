package esw.agent.akka.app.process

object ProcessUtils {
  def isInstalled(cmd: String): Boolean = new ProcessBuilder("bash", "-c", s"command -v $cmd").start().waitFor() == 0
}
