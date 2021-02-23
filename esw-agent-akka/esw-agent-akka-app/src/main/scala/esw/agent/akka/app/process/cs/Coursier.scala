package esw.agent.akka.app.process.cs

import esw.agent.akka.client.models.ContainerConfig

object Coursier {
  lazy val cs: String = "cs"

  def ocsApp(version: Option[String], gcMetricsEnabled: Boolean): CoursierLaunch =
    CoursierLaunch("ocs-app", version, gcMetricsEnabled)

  def smApp(version: Option[String], gcMetricsEnabled: Boolean): CoursierLaunch =
    CoursierLaunch("sequence-manager", version, gcMetricsEnabled)

  def containerApp(config: ContainerConfig, gcMetricsEnabled: Boolean): CoursierLaunch = {
    val appName = s"${config.orgName}::${config.deployModule}"
    CoursierLaunch(appName, Some(config.version), gcMetricsEnabled)
  }
}
