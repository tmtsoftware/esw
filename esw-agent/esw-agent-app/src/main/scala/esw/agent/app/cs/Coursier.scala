package esw.agent.app.cs

object Coursier {
  private def csNative             = "cs"
  private def csLauncher           = getClass.getResource("/coursier").getPath
  private def csInstalled: Boolean = new ProcessBuilder("command", "-v", csNative).start().waitFor() == 0

  lazy val cs: String = if (csInstalled) csNative else csLauncher

  def ocsApp(version: String): CoursierLaunch = CoursierLaunch("ocs-app", version)
}
