package esw.agent.akka.app.process.cs

import esw.agent.akka.app.BuildInfo
import esw.agent.akka.client.models.ContainerConfig

/**
 * This is a convenience utility which can be used to spawn OCS App, Sequence Manager & Container App via Coursier.
 */
object Coursier {
  lazy val cs: String = "cs"

  def ocsApp(version: Option[String]): CoursierLaunch = CoursierLaunch("esw-ocs-app", version)

  def smApp(version: Option[String]): CoursierLaunch = CoursierLaunch("esw-sm-app", version)

  def containerApp(config: ContainerConfig): CoursierLaunch = {
    val appName = s"${config.orgName}:${config.deployModule}_${BuildInfo.scalaBinaryVersion}"
    CoursierLaunch(appName, Some(config.version))
  }
}
