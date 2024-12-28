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
      organization     := "com.github.tmtsoftware.esw",
      organizationName := "TMT Org",
//      dependencyOverrides += PekkoHttp.`pekko-http-spray-json`,
//      dependencyOverrides += Libs.`slf4j-api`,
      scalaVersion := EswKeys.scalaVersion,
      scmInfo      := Some(ScmInfo(url(EswKeys.homepageValue), "git@github.com:tmtsoftware/esw.git")),
      // ======== sbt-docs Settings =========
      docsRepo       := "https://github.com/tmtsoftware/tmtsoftware.github.io.git",
      docsParentDir  := EswKeys.projectName,
      gitCurrentRepo := "https://github.com/tmtsoftware/esw",
      // ================================
      resolvers += "Apache Pekko Staging".at("https://repository.apache.org/content/groups/staging"),
//      resolvers += "Apache Pekko Snapshots".at("https://repository.apache.org/content/groups/snapshots"),
      resolvers += "jitpack" at "https://jitpack.io",
      resolvers += Resolver.mavenLocal, // required to resolve kotlin `examples` deps published locally
      autoCompilerPlugins := true,
      scalacOptions ++= Seq(
        "-encoding",
        "UTF-8",
        "-feature",
        "-unchecked",
        "-deprecation",
//        "-rewrite",
//        "-source",
//        "3.4-migration"
      ),
      licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
      Test / testOptions ++= reporterOptions,
      Test / packageBin / publishArtifact := true,
      // jitpack provides the env variable VERSION=<version being built> # A tag or commit. We have aliased VERSION to JITPACK_VERSION
      // we make use of it so that the version in class metadata (e.g. classOf[HttpService].getPackage.getSpecificationVersion)
      // and the maven repo match
      version := sys.env.getOrElse("JITPACK_VERSION", "0.1.0-SNAPSHOT"),
      fork    := true,
      javaOptions += "-Xmx2G",
      Test / fork := true,
      Test / javaOptions ++= Seq(
        "-Dpekko.actor.serialize-messages=on",
        // This option is needed to look in the esw/examples subproject for Kotlin scripts instead of in the
        // sequencer-scripts repo (via osw-apps)
        "-Dtest.esw=true",
        // These are needed when using jdk 21 and Blockhound
        "-XX:+AllowRedefinitionToAddDeleteMethods",
        "-XX:+EnableDynamicAgentLoading",
        // Set to true to use python scripting instead of kotlin
        // (make sure to set environment variables CSW_PYTHON and PYTHONPATH to the top level csw-python directory)
//        "-DenableEswPythonScripting=true"
      ),
      Global / cancelable     := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
      scalafmtOnCompile       := true,
      unidocGenjavadocVersion := "0.18",
      commands += Command.command("openSite") { state =>
        val uri = s"file://${Project.extract(state).get(siteDirectory)}/${docsParentDir.value}/${version.value}/index.html"
        state.log.info(s"Opening browser at $uri ...")
        java.awt.Desktop.getDesktop.browse(new java.net.URI(uri))
        state
      },
      Global / excludeLintKeys := Set(
        SettingKey[Boolean]("ide-skip-project"),
        aggregate,              // verify if this needs to be here or our configuration is wrong
        unidocGenjavadocVersion // verify if this needs to be here or our configuration is wrong
      )
    )
}
