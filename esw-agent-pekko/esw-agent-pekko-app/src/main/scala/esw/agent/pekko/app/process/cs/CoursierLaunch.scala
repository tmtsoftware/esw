package esw.agent.pekko.app.process.cs

case class CoursierLaunch(appName: String, appVersion: Option[String]) {
  private val app = appVersion.fold(appName)(version => s"$appName:$version")

  def launch(repos: List[String], mainClass: String, args: List[String]): List[String] = {
    val testOpt = sys.props.get("test.esw").getOrElse("false")
    val xxx =
      List(Coursier.cs, "launch", "-D", s"test.esw=$testOpt") ::: app :: "-r" :: repos ::: "-M" :: mainClass :: "--" :: args
    println(s"XXX CoursierLaunch: ${xxx.mkString(" ")}")
    xxx
  }

  // cs launch --channel url://apps.json esw-ocs-app:2.0.0 -- start seqcomp
  def launch(channel: String, args: List[String]): List[String] = {
    val testOpt = sys.props.get("test.esw").getOrElse("false")
    val xxx     = List(Coursier.cs, "launch", "-D", s"test.esw=$testOpt") ::: "--channel" :: channel :: app :: "--" :: args
    println(s"XXX CoursierLaunch: ${xxx.mkString(" ")}")
    xxx
  }

  def fetch(channel: String): List[String] = List(Coursier.cs, "fetch") ::: "--channel" :: channel :: app :: Nil
}
