package esw.agent.akka.app.process.cs

object Coursier {
  lazy val cs: String = "cs"

  def ocsApp(version: Option[String]): CoursierLaunch           = CoursierLaunch("ocs-app", version)
  def smApp(version: Option[String]): CoursierLaunch            = CoursierLaunch("sequence-manager", version)
  def locationAgentApp(version: Option[String]): CoursierLaunch = CoursierLaunch("location-agent", version)
}
