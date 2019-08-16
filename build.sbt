lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `esw-ocs`,
    `esw-gateway-server`,
    `esw-http-core`,
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
    `esw-ocs-api`,
    `esw-ocs-impl`,
    `esw-ocs-macros`,
    `esw-ocs-app`
  )

lazy val `esw-ocs-api` = project
  .in(file("esw-ocs/esw-ocs-api"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsApi.value
  )

lazy val `esw-ocs-impl` = project
  .in(file("esw-ocs/esw-ocs-impl"))
//  .enablePlugins(MaybeCoverage) fixme: enable
  .settings(
    libraryDependencies ++= Dependencies.OcsImpl.value
  )
  .dependsOn(`esw-ocs-api` % "compile->compile;test->test", `esw-ocs-macros`, `esw-utils`)

lazy val `esw-ocs-macros` = project
  .in(file("esw-ocs/esw-ocs-macros"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsMacros.value
  )

lazy val `esw-ocs-app` = project
  .in(file("esw-ocs/esw-ocs-app"))
  .enablePlugins(EswBuildInfo, DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.OcsApp.value
  )
  .dependsOn(`esw-ocs-impl`)

lazy val `esw-http-core` = project
  .in(file("esw-http-core"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.EswHttpCore.value
  )
  .dependsOn(`msocket-core`)

//fixme: Enable MaybeCoverage
lazy val `esw-gateway-server` = project
  .in(file("esw-gateway-server"))
  .enablePlugins(EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.GatewayServer.value
  )
  .dependsOn(`esw-http-core` % "compile->compile;test->test")

lazy val `esw-integration-test` = project
  .in(file("esw-integration-test"))
  .settings(libraryDependencies ++= Dependencies.IntegrationTest.value)
  .settings(fork in Test := true)
  .dependsOn(
    `esw-gateway-server` % "test->compile;test->test",
    `esw-http-core`      % "test->compile;test->test",
    `esw-ocs-impl`       % "test->compile;test->test",
    `esw-ocs-app`
  )

lazy val `esw-utils` = project
  .in(file("esw-utils"))
  .settings(libraryDependencies ++= Dependencies.Utils.value)
  .dependsOn(`esw-ocs-api`)

lazy val `msocket-core` = project
  .in(file("msocket-core"))
  .enablePlugins(PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.MsocketCore.value
  )

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)
