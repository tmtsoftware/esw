package esw.agent.akka.app.process.cs

import java.io.File

case class CoursierLaunch(appName: String, appVersion: Option[String], gcMetricsEnabled: Boolean) {
  private val app = appVersion.fold(appName)(version => s"$appName:$version")

  def launch(repos: List[String], mainClass: String, args: List[String]): List[String] = {
    val gcFlagList = if (gcMetricsEnabled) getGCFlags else List.empty[String]
    List(Coursier.cs, "launch") ::: gcFlagList ::: app :: "-r" :: repos ::: "-M" :: mainClass :: "--" :: args
  }

  // cs launch --channel url://apps.json ocs-app:2.0.0 -- start seqcomp
  def launch(channel: String, args: List[String]): List[String] = {
    val gcFlagList = if (gcMetricsEnabled) getGCFlags else List.empty[String]
    List(Coursier.cs, "launch") ::: gcFlagList ::: "--channel" :: channel :: app :: "--" :: args
  }

  def fetch(channel: String): List[String] = List(Coursier.cs, "fetch") ::: "--channel" :: channel :: app :: Nil

  private def getGCFlags: List[String] = {
    val gcLogDirString = System.getProperty("user.home") + "/gc-metrics"
    new File(gcLogDirString).mkdir()
    val gcLogFileName = gcLogDirString + "/" + appName + "_gc.txt"
    List("--java-opt", s"-Xlog:gc:file=$gcLogFileName::filecount=1")
  }
}
