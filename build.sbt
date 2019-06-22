lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `ocs-framework`,
    `async-macros`,
    `gateway`,
    `template`
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

lazy val `ocs` = project
  .in(file("ocs"))
  .aggregate(
    `ocs-framework`,
    `async-macros`
  )

lazy val `ocs-framework` = project
  .in(file("ocs/ocs-framework"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.`ocs-framework`.value
  )
  .dependsOn(`async-macros`)

lazy val `async-macros` = project
  .in(file("ocs/async-macros"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.`async-macros`.value
  )

lazy val `template` = project
  .in(file("template"))
  .aggregate(`http-server`)

lazy val `http-server` = project
  .in(file("template/http-server"))
  .enablePlugins(MaybeCoverage)
  .enablePlugins(EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.`http-server`.value
  )

lazy val `gateway` = project
  .in(file("gateway"))
  .aggregate(`gateway-server`)

lazy val `gateway-server` = project
  .in(file("gateway/gateway-server"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.`gateway-server`.value
  )
  .dependsOn(`http-server`)

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)
