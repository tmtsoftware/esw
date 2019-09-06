import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `esw-utils`,
    `esw-ocs`,
    `esw-http-core`,
    `esw-integration-test`,
    `esw-gateway`
  )

lazy val githubReleases: Seq[ProjectReference]   = Seq.empty
lazy val unidocExclusions: Seq[ProjectReference] = Seq(`esw-integration-test`, `esw-ocs-api`.js)

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
    `esw-ocs-api`.js,
    `esw-ocs-api`.jvm,
    `esw-ocs-client`,
    `esw-ocs-impl`,
    `esw-ocs-macros`,
    `esw-ocs-app`
  )

lazy val `esw-ocs-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("esw-ocs/esw-ocs-api"))
  .jvmConfigure(_.enablePlugins(MaybeCoverage))
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.OcsApi.value
  )

lazy val `esw-ocs-client` = project
  .in(file("esw-ocs/esw-ocs-client"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsClient.value
  )
  .dependsOn(`esw-ocs-api`.jvm % "compile->compile;test->test")

lazy val `esw-ocs-impl` = project
  .in(file("esw-ocs/esw-ocs-impl"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsImpl.value
  )
  .dependsOn(`esw-ocs-api`.jvm % "compile->compile;test->test", `esw-ocs-client`, `esw-ocs-macros`, `esw-utils`)

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
  .dependsOn(`esw-ocs-impl`, `esw-http-core` % "compile->compile;test->test")

lazy val `esw-http-core` = project
  .in(file("esw-http-core"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.EswHttpCore.value
  )

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
  .dependsOn(`esw-ocs-api`.jvm % "compile->compile;test->test", `esw-ocs-macros`)

lazy val `esw-gateway` = project
  .aggregate(
    `esw-gateway-api`,
    `esw-gateway-impl`,
    `esw-gateway-server`
  )

lazy val `esw-gateway-api` = project
  .in(file("esw-gateway/esw-gateway-api"))
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayApi.value
  )

lazy val `esw-gateway-impl` = project
  .in(file("esw-gateway/esw-gateway-impl"))
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayImpl.value
  )
  .dependsOn(`esw-gateway-api`)

lazy val `esw-gateway-server` = project
  .in(file("esw-gateway/esw-gateway-server"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayServer.value
  )
  .dependsOn(`esw-gateway-impl`, `esw-http-core` % "compile->compile;test->test")

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)
