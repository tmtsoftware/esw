import sbt.Keys.update
import sbt.{AutoPlugin, Def, Keys, Plugins, Setting, Task, plugins}

import scala.sys.process.stringToProcess

object KotlinPlugin extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    update := {
      publishKotlinTask.value
      update.value
    }
  )

  private lazy val publishKotlinTask: Def.Initialize[Task[Unit]] =
    Def.task {
      runKotlin(Seq("publishToMavenLocal"))
    }

  private def runKotlin(args: Seq[String]): Unit =
    s"./gradle.sh ${args.mkString(" ")}".lineStream_!
      .foreach(msg => println(s"[Kotlin] $msg"))
}
