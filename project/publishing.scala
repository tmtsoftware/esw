import java.io.File

import sbt.Keys._
import sbt._

object NoPublish extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
}

object PublishBintray extends AutoPlugin {
  import bintray.BintrayPlugin
  import BintrayPlugin.autoImport._

  override def requires: Plugins = BintrayPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    bintrayOrganization := Some("twtmt"),
    bintrayPackage := name.value
  )
}

object DeployApp extends AutoPlugin {
  import com.typesafe.sbt.packager.SettingsHelper
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
  import com.typesafe.sbt.packager.universal.UniversalPlugin
  import UniversalPlugin.autoImport.{Universal, UniversalDocs}

  override def requires: Plugins = UniversalPlugin && JavaAppPackaging && PublishBintray && EswBuildInfo

  override def projectSettings: Seq[Setting[_]] =
    SettingsHelper.makeDeploymentSettings(Universal, packageBin in Universal, "zip") ++
      SettingsHelper.makeDeploymentSettings(UniversalDocs, packageBin in UniversalDocs, "zip") ++ Seq(
      target in Universal := file(".") / "target" / "universal"
    )
}

// used only in DeployApp plugin
object EswBuildInfo extends AutoPlugin {
  import sbtbuildinfo.BuildInfoPlugin
  import BuildInfoPlugin.autoImport._

  override def requires: Plugins = BuildInfoPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    // module name(e.g. module-name) gets converted into package name(e.g. module.name) for buildInfo, so it does not have
    // same package across all modules in the repo
    buildInfoPackage := name.value.replace('-', '.')
  )
}
