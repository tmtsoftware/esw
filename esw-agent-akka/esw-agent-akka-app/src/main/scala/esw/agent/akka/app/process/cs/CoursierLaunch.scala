package esw.agent.akka.app.process.cs

import java.io.File

import scala.util.Try

// TODO refactor
case class CoursierLaunch(appName: String, appVersion: Option[String]) {
  private val app = appVersion.fold(appName)(version => s"$appName:$version")

  // cs launch --channel url://apps.json ocs-app:2.0.0 -- start seqcomp
  def launch(channel: String, args: List[String]): List[String] = {
    val indexOfComponentName = args.indexOf("-n") + 1
    val gcLogDirString       = System.getProperty("user.home") + "/gc-perf-latest"
    Try {
      new File(gcLogDirString).mkdir()
    }.recover(e => s"Directory creation failed due to : ${e.getMessage}")
    val gcLogFileName = gcLogDirString + "/" + args(indexOfComponentName) + "_seqComp_gc.txt"
    // TODO remove after ESW-182 testing (This is to capture logs of sequence components)
    val command = List(
      Coursier.cs,
      "launch"
    ) ::: List("--java-opt", s"-Xlog:gc:file=$gcLogFileName::filecount=1") ::: "--channel" :: channel :: app :: "--" :: args
    command
  }

  def fetch(channel: String): List[String] = List(Coursier.cs, "fetch") ::: "--channel" :: channel :: app :: Nil
}
