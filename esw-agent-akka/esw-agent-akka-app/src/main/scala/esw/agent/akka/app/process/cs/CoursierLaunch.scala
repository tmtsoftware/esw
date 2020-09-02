package esw.agent.akka.app.process.cs

case class CoursierLaunch(appName: String, appVersion: Option[String]) {
  private val app = appVersion.fold(appName)(version => s"$appName:$version")

  // cs launch --channel url://apps.json ocs-app:2.0.0 -- start seqcomp
  def launch(channel: String, args: List[String]): List[String] = {
    val value = List(Coursier.cs, "launch") ::: "--channel" :: channel :: app :: "--" :: args
    println(value.mkString(" "))
    value
  }

  def fetch(channel: String): List[String] = List(Coursier.cs, "fetch") ::: "--channel" :: channel :: app :: Nil
}
