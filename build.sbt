import sbt.Test

lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `ocs-framework`,
    `async-macros`,
    `gateway`,
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
  .settings(
    libraryDependencies ++= Dependencies.`ocs-framework`.value
  )
  .dependsOn(`async-macros`)

lazy val `async-macros` = project
  .in(file("ocs/async-macros"))
  .settings(
    libraryDependencies ++= Dependencies.`async-macros`.value
  )

lazy val `template` = project
  .in(file("template"))
  .aggregate(`http-server`, `http-server-tests`)

lazy val `http-server` = project
  .in(file("template/http-server"))
  .settings(
    libraryDependencies ++= Dependencies.`http-server`.value
  )

lazy val `http-server-tests` = project
  .in(file("template/http-server-tests"))
  .settings(
    libraryDependencies ++= Dependencies.`http-server-tests`.value,
    Test / sourceDirectory := baseDirectory.value / "src" / "main"
  )
  .dependsOn(`http-server`)

lazy val `gateway` = project
  .in(file("gateway"))
  .aggregate(`gateway-server`, `gateway-server-tests`)

lazy val `gateway-server` = project
  .in(file("gateway/gateway-server"))
  .settings(
    libraryDependencies ++= Dependencies.`gateway-server`.value
  )
  .dependsOn(`http-server`)

lazy val `gateway-server-tests` = project
  .in(file("gateway/gateway-server-tests"))
  .settings(
    libraryDependencies ++= Dependencies.`gateway-server-tests`.value,
    Test / sourceDirectory := baseDirectory.value / "src" / "main"
  )
  .dependsOn(`gateway-server`)

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)
