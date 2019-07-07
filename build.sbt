lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `esw-ocs`,
    `esw-gateway-server`,
    `esw-template`,
    `esw-integration-test`
  )

lazy val githubReleases: Seq[ProjectReference]   = Seq.empty
lazy val unidocExclusions: Seq[ProjectReference] = Seq(`esw-integration-test`)

val enableCoverage         = sys.props.get("enableCoverage").contains("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val esw = (project in file("."))
  .aggregate(aggregateProjects: _*)
  .enablePlugins(NoPublish, UnidocSite, GithubPublishDocs, GitBranchPrompt, GithubRelease)
  .disablePlugins(BintrayPlugin)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.addAliases)
  .settings(Settings.docExclusions(unidocExclusions))
//  .settings(GithubRelease.githubReleases(githubReleases))

lazy val `esw-ocs` = project
  .in(file("esw-ocs"))
  .aggregate(
    `esw-ocs-framework-api`,
    `esw-ocs-framework`,
    `esw-async-macros`
  )
lazy val `esw-ocs-framework-api` = project
  .in(file("esw-ocs/esw-ocs-framework-api"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsFrameworkApi.value
  )

lazy val `esw-ocs-framework` = project
  .in(file("esw-ocs/esw-ocs-framework"))
//  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsFramework.value
  )
  .dependsOn(`esw-ocs-framework-api`, `esw-async-macros`)

lazy val `esw-async-macros` = project
  .in(file("esw-ocs/esw-async-macros"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.AsyncMacros.value
  )

lazy val `esw-template` = project
  .in(file("esw-template"))
  .aggregate(`esw-template-http-server`)

lazy val `esw-template-http-server` = project
  .in(file("esw-template/esw-template-http-server"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.TemplateHttpServer.value
  )

lazy val `esw-gateway-server` = project
  .in(file("esw-gateway-server"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.GatewayServer.value
  )
  .dependsOn(`esw-template-http-server` % "compile->compile;test->test")

lazy val `esw-integration-test` = project
  .in(file("esw-integration-test"))
  .settings(libraryDependencies ++= Dependencies.IntegrationTest.value)
  .settings(fork in Test := true)
  .dependsOn(
    `esw-gateway-server`       % "test->compile;test->test",
    `esw-template-http-server` % "test->compile;test->test"
  )

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)
