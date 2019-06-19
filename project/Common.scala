import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt.librarymanagement.ScmInfo
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Setting, SettingKey, Test, Tests, settingKey, url, _}
import sbtunidoc.GenJavadocPlugin.autoImport.unidocGenjavadocVersion

object Common extends AutoPlugin {

  // enable these values to be accessible to get and set in sbt console
  object autoImport {
    val enableFatalWarnings: SettingKey[Boolean] = settingKey[Boolean]("enable fatal warnings")
  }

  override def trigger = allRequirements

  override def requires = JvmPlugin

  import autoImport._
  override def globalSettings: Seq[Setting[_]] = Seq(
    organization := "com.github.tmtsoftware.esw",
    organizationName := "TMT Org",
    scalaVersion := EswKeys.scalaVersion,
    scmInfo := Some(
      ScmInfo(url(EswKeys.homepageValue), "git@github.com:tmtsoftware/esw.git")
    ),
    resolvers += "jitpack" at "https://jitpack.io",
    resolvers += "bintray" at "http://jcenter.bintray.com",
    enableFatalWarnings := false,
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      if (enableFatalWarnings.value) "-Xfatal-warnings" else "",
      "-Xlint:_,-missing-interpolator",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture"
      //      "-Xprint:typer"
    ),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    testOptions in Test ++= Seq(
      // show full stack traces and test case durations
      Tests.Argument("-oDF")
    ),
    publishArtifact in (Test, packageBin) := true,
    version := {
      sys.props.get("prod.publish") match {
        case Some("true") => version.value
        case _            => "0.1-SNAPSHOT"
      }
    },
    isSnapshot := !sys.props.get("prod.publish").contains("true"),
    autoCompilerPlugins := true,
    cancelable in Global := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
    scalafmtOnCompile := true,
    unidocGenjavadocVersion := "0.13"
  )
}
