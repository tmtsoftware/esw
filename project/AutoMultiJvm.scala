import com.typesafe.sbt.MultiJvmPlugin.autoImport._
import com.typesafe.sbt.SbtMultiJvm
import sbt.Keys.{artifacts, moduleName, packageBin, packagedArtifacts}
import sbt.{Def, _}
import sbtassembly.AssemblyKeys._
import sbtassembly.MergeStrategy

object AutoMultiJvm extends AutoPlugin {

  override def projectSettings: Seq[Setting[_]] =
    SbtMultiJvm.multiJvmSettings ++ Seq(
      multiNodeHosts in MultiJvm := multiNodeHostNames,
      assemblyMergeStrategy in assembly in MultiJvm := {
        case "application.conf"                     => MergeStrategy.concat
        case x if x.contains("versions.properties") => MergeStrategy.discard
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly in MultiJvm).value
          oldStrategy(x)
      }
    )

  override def projectConfigurations: Seq[Configuration] = List(MultiJvm)

  private def multiNodeHostNames = sys.env.get("multiNodeHosts") match {
    case Some(str) => str.split(",").toSeq
    case None      => Seq.empty
  }

}
