import com.typesafe.sbt.MultiJvmPlugin.autoImport._
import com.typesafe.sbt.SbtMultiJvm
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin.autoImport
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.headerSettings
import sbt.Keys.fork
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.MergeStrategy

object AutoMultiJvm extends AutoPlugin {

  private val copyrightHeaderSettings: Seq[Setting[_]] = autoImport.automateHeaderSettings(MultiJvm) ++ headerSettings(MultiJvm)

  override def projectSettings: Seq[Setting[_]] = {
    SbtMultiJvm.multiJvmSettings ++ Seq(
      MultiJvm / multiNodeHosts := multiNodeHostNames,
      MultiJvm / fork           := true,
      MultiJvm / assembly / assemblyMergeStrategy := {
        case "application.conf"                     => MergeStrategy.concat
        case x if x.contains("versions.properties") => MergeStrategy.discard
        case x if x.contains("schema")              => MergeStrategy.last
        case x if x.contains("main.kotlin_module")  => MergeStrategy.concat
        case x =>
          val oldStrategy = (MultiJvm / assembly / assemblyMergeStrategy).value
          oldStrategy(x)
      }
    ) ++ copyrightHeaderSettings
  }

  override def projectConfigurations: Seq[Configuration] = List(MultiJvm)

  private def multiNodeHostNames =
    sys.env.get("multiNodeHosts") match {
      case Some(str) => str.split(",").toSeq
      case None      => Seq.empty
    }

}
