import org.tmt.sbt.docs.DocKeys._
import org.tmt.sbt.docs.{Settings => DocSettings}
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

docsRepo in ThisBuild := "git@github.com:tmtsoftware/tmtsoftware.github.io.git"
docsParentDir in ThisBuild := EswKeys.projectName
gitCurrentRepo in ThisBuild := "https://github.com/tmtsoftware/esw"

lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `esw-ocs`,
    `esw-http-core`,
    `esw-integration-test`,
    `esw-gateway`
  )

lazy val githubReleases: Seq[ProjectReference] = Seq.empty
lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `esw-integration-test`,
  `esw-ocs-api`.js,
  `esw-gateway-api`.js
)

val enableCoverage         = sys.props.get("enableCoverage").contains("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val esw = (project in file("."))
  .aggregate(aggregateProjects: _*)
  .enablePlugins(NoPublish, UnidocSitePlugin, GithubPublishPlugin, GitBranchPrompt, GithubRelease)
  .disablePlugins(BintrayPlugin)
  .settings(DocSettings.makeSiteMappings(docs))
  .settings(Settings.addAliases)
  .settings(DocSettings.docExclusions(unidocExclusions))
//  .settings(GithubRelease.githubReleases(githubReleases))

lazy val `esw-ocs` = project
  .in(file("esw-ocs"))
  .aggregate(
    `esw-ocs-api`.js,
    `esw-ocs-api`.jvm,
    `esw-ocs-dsl`,
    `esw-ocs-impl`,
    `esw-ocs-app`
  )

lazy val `esw-ocs-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("esw-ocs/esw-ocs-api"))
  .jvmConfigure(_.enablePlugins(MaybeCoverage, PublishBintray).dependsOn(`esw-test-reporter` % Test))
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.OcsApi.value
  )

lazy val `esw-ocs-impl` = project
  .in(file("esw-ocs/esw-ocs-impl"))
  .enablePlugins(MaybeCoverage, PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.OcsImpl.value
  )
  .dependsOn(
    `esw-ocs-api`.jvm % "compile->compile;test->test",
    `esw-ocs-dsl`,
    `esw-test-reporter` % Test
  )

lazy val `esw-ocs-app` = project
  .in(file("esw-ocs/esw-ocs-app"))
  .enablePlugins(EswBuildInfo, DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsApp.value
  )
  .dependsOn(
    `esw-ocs-impl`      % "compile->compile;test->test",
    `esw-http-core`     % "compile->compile;test->test",
    `esw-test-reporter` % Test
  )

lazy val `esw-http-core` = project
  .in(file("esw-http-core"))
  .enablePlugins(PublishBintray, MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.EswHttpCore.value
  )
  .dependsOn(`esw-test-reporter` % Test)

lazy val `esw-integration-test` = project
  .in(file("esw-integration-test"))
  .settings(libraryDependencies ++= Dependencies.IntegrationTest.value)
  .settings(fork in Test := true)
  .settings(
    update := {
      publishKotlin.value
      update.value
    }
  )
  .dependsOn(
    `esw-gateway-server` % "test->compile;test->test",
    `esw-http-core`      % "test->compile;test->test",
    `esw-ocs-impl`       % "test->compile;test->test",
    `esw-ocs-app`,
    `esw-test-reporter` % Test
  )

import scala.sys.process._
lazy val publishKotlin: Def.Initialize[Task[Unit]] = Def.task {
  "./esw-kt/publish.sh".lineStream_!
    .foreach(msg => println(s"[Kotlin] $msg"))
}

lazy val `esw-ocs-dsl` = project
  .in(file("esw-ocs/esw-ocs-dsl"))
  .settings(libraryDependencies ++= Dependencies.Utils.value)
  .dependsOn(`esw-ocs-api`.jvm % "compile->compile;test->test", `esw-test-reporter` % Test)

lazy val `esw-gateway` = project
  .aggregate(
    `esw-gateway-api`.jvm,
    `esw-gateway-api`.js,
    `esw-gateway-impl`,
    `esw-gateway-server`
  )

lazy val `esw-gateway-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("esw-gateway/esw-gateway-api"))
  .jvmConfigure(_.dependsOn(`esw-test-reporter` % Test))
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayApi.value
  )

lazy val `esw-gateway-impl` = project
  .in(file("esw-gateway/esw-gateway-impl"))
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayImpl.value
  )
  .dependsOn(`esw-gateway-api`.jvm, `esw-test-reporter` % Test)

lazy val `esw-gateway-server` = project
  .in(file("esw-gateway/esw-gateway-server"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayServer.value
  )
  .dependsOn(`esw-gateway-impl`, `esw-http-core` % "compile->compile;test->test", `esw-test-reporter` % Test)

lazy val `esw-test-reporter` = project
  .in(file("esw-test-reporter"))
  .settings(libraryDependencies += Libs.scalatest)

lazy val `esw-sm` = project
  .in(file("esw-sm"))
  .settings(
    libraryDependencies ++= Dependencies.EswSM.value
  )
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-ocs-dsl`,
    `esw-http-core`,
    `esw-ocs-impl`
  )
/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxMaterialSitePlugin)
