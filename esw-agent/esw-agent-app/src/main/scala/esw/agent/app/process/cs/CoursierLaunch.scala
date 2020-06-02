package esw.agent.app.process.cs

case class CoursierLaunch(appName: String, appVersion: String) {
  private val app                               = s"$appName:$appVersion"
  private def javaOpts(_javaOpts: List[String]) = _javaOpts.flatMap(List("-J", _))

  // cs launch -J -Dxmx=5M --channel url://apps.json ocs-app:2.0.0 -- start seqcomp
  def launch(channel: String, args: List[String], _javaOpts: List[String] = List.empty): List[String] = {
    List(Coursier.cs, "launch") ++ javaOpts(_javaOpts) ::: "--channel" :: channel :: app :: "--" :: args
  }
}
