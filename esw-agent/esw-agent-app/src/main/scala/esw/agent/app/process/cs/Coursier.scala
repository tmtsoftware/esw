package esw.agent.app.process.cs

object Coursier {
  lazy val cs: String = "cs"

  def ocsApp(version: Option[String]): CoursierLaunch = CoursierLaunch("ocs-app", version)
}
