import sbt.Keys.compile
import sbt.librarymanagement.Configurations.Compile
import sbt.{AutoPlugin, Def, Plugins, Setting, Task, plugins}

import scala.sys.process.stringToProcess

object KotlinPlugin extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    compile in Compile := {
      publishKotlinTask.value
      (compile in Compile).value
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
