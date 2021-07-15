import com.typesafe.sbt.site.SitePlugin.autoImport.siteDirectory
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import org.tmt.sbt.docs.DocKeys._
import sbt.Keys._
import sbt._
import sbt.librarymanagement.ScmInfo
import sbtunidoc.GenJavadocPlugin.autoImport.unidocGenjavadocVersion

object Common {
  private val enableFatalWarnings: Boolean = sys.props.get("enableFatalWarnings").contains("true")
  private val storyReport: Boolean         = sys.props.get("generateStoryReport").contains("true")
  private val enableCoverage               = sys.props.get("enableCoverage").contains("true")

  private val reporterOptions: Seq[Tests.Argument] =
    // "-oDF" - show full stack traces and test case durations
    // -C - to generate CSV story and test mapping
    if (storyReport)
      Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-oDF", "-C", "tmt.test.reporter.TestReporter"),
        Tests.Argument(TestFrameworks.JUnit)
      )
    else Seq(Tests.Argument("-oDF"))

  val jsTestArg              = Test / testOptions := Seq(Tests.Argument("-oDF"))
  val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

  lazy val CommonSettings: Seq[Setting[_]] =
    Seq(
      organization := "com.github.tmtsoftware.esw",
      organizationName := "TMT Org",
      dependencyOverrides += AkkaHttp.`akka-http-spray-json`,
      dependencyOverrides += Libs.`slf4j-api`,
      scalaVersion := EswKeys.scalaVersion,
      scmInfo := Some(ScmInfo(url(EswKeys.homepageValue), "git@github.com:tmtsoftware/esw.git")),
      // ======== sbt-docs Settings =========
      docsRepo := "https://github.com/tmtsoftware/tmtsoftware.github.io.git",
      docsParentDir := EswKeys.projectName,
      gitCurrentRepo := "https://github.com/tmtsoftware/esw",
      // ================================
      resolvers += "jitpack" at "https://jitpack.io",
      resolvers += Resolver.mavenLocal,
      autoCompilerPlugins := true,
      scalacOptions ++= Seq(
        "-encoding",
        "UTF-8",
        "-feature",
        "-unchecked",
        "-deprecation",
        //-W Options
        "-Wdead-code",
        if (enableFatalWarnings) "-Wconf:any:error" else "-Wconf:any:warning-verbose",
        //-X Options
        "-Xlint:_,-missing-interpolator",
        "-Xsource:3",
        "-Xcheckinit",
        "-Xasync"
        // -Y options are rarely needed, please look for -W equivalents
      ),
      licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
      Test / testOptions ++= reporterOptions,
      Test / packageBin / publishArtifact := true,
      version := {
        sys.props.get("prod.publish") match {
          case Some("true") => version.value
          case _            => "0.1.0-SNAPSHOT"
        }
      },
      fork := true,
      Test / fork := false,
      isSnapshot := !sys.props.get("prod.publish").contains("true"),
      Test / javaOptions ++= Seq("-Dakka.actor.serialize-messages=on"),
      cancelable in Global := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
      scalafmtOnCompile := true,
      unidocGenjavadocVersion := "0.16",
      commands += Command.command("openSite") { state =>
        val uri = s"file://${Project.extract(state).get(siteDirectory)}/${docsParentDir.value}/${version.value}/index.html"
        state.log.info(s"Opening browser at $uri ...")
        java.awt.Desktop.getDesktop.browse(new java.net.URI(uri))
        state
      },
      Global / excludeLintKeys := Set(
        SettingKey[Boolean]("ide-skip-project"),
        aggregate,              //verify if this needs to be here or our configuration is wrong
        unidocGenjavadocVersion //verify if this needs to be here or our configuration is wrong
      )
    )
}
