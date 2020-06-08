package esw.agent.app.process.cs

case class CoursierLaunch(appName: String, appVersion: Option[String]) {
  private val app = appVersion.fold(appName)(version => s"$appName:$version")

  // cs launch --channel url://apps.json ocs-app:2.0.0 -- start seqcomp
  def launch(channel: String, args: List[String]): List[String] =
    List(Coursier.cs, "launch") ::: "--channel" :: channel :: app :: "--" :: args
}
