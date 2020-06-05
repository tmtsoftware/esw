package esw.agent.app.process.cs

import esw.agent.app.process.ProcessUtils

object Coursier {
  private def csNative             = "cs"
  private def csLauncher           = getClass.getResource("/coursier").getPath
  private def csInstalled: Boolean = ProcessUtils.isInstalled(csNative, "--version")

  lazy val cs: String = if (csInstalled) csNative else csLauncher

  def ocsApp(version: Option[String]): CoursierLaunch = CoursierLaunch("ocs-app", version)
}
