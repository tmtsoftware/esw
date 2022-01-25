import sbt.Keys._
import sbt._

object NoPublish extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      publishArtifact := false,
      publish         := {},
      publishLocal    := {}
    )
}

object EswBuildInfo extends AutoPlugin {

  import sbtbuildinfo.BuildInfoPlugin
  import BuildInfoPlugin.autoImport._

  override def requires: Plugins = BuildInfoPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaBinaryVersion),
      // module name(e.g. module-name) gets converted into package name(e.g. module.name) for buildInfo, so it does not have
      // same package across all modules in the repo
      buildInfoPackage := name.value.replace('-', '.')
    )
}
