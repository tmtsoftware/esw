lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `esw-ocs`,
    `esw-gateway`,
    `esw-template`
  )

lazy val githubReleases: Seq[ProjectReference]   = Seq.empty
lazy val unidocExclusions: Seq[ProjectReference] = Seq.empty

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
    `ocs-framework-api`,
    `ocs-framework`,
    `async-macros`
  )
lazy val `ocs-framework-api` = project
  .in(file("esw-ocs/ocs-framework-api"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsFrameworkApi.value
  )

lazy val `ocs-framework` = project
  .in(file("esw-ocs/ocs-framework"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsFramework.value
  )
  .dependsOn(`async-macros`)

lazy val `async-macros` = project
  .in(file("esw-ocs/async-macros"))
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

lazy val `esw-gateway` = project
  .in(file("esw-gateway"))
  .aggregate(`esw-gateway-server`)

lazy val `esw-gateway-server` = project
  .in(file("esw-gateway/esw-gateway-server"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.GatewayServer.value
  )
  .dependsOn(`esw-template-http-server`)

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)
