import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{licenses, scmInfo, _}
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Global, ScmInfo, Setting, Test, Tests, settingKey, toRepositoryName, url}
import sbtunidoc.GenJavadocPlugin.autoImport.unidocGenjavadocVersion

class Commons extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = JvmPlugin

  val enableFatalWarnings = settingKey[Boolean]("enable fatal warnings")

  override def globalSettings: Seq[Setting[_]] = Seq(
    organization := "com.github.tmtsoftware.esw",
    organizationName := "TMT Org",
    organizationHomepage := Some(url("https://www.github.com/tmtsoftware/esw")),
    homepage := Some(url(EswKeys.homepageValue)),
    description := "Executive Software",
    scalaVersion := EswKeys.scalaVersion,
    scmInfo := Some(
      ScmInfo(url(EswKeys.homepageValue), "git@github.com:tmtsoftware/esw.git")
    ),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    resolvers += "jitpack" at "https://jitpack.io",
    resolvers += "bintray" at "http://jcenter.bintray.com",
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
    fork := true,
    enableFatalWarnings := false,
    autoCompilerPlugins := true,
    cancelable in Global := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
    scalafmtOnCompile := true,
    unidocGenjavadocVersion := "0.13"
  )
}
