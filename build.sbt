lazy val aggregateProjects: Seq[ProjectReference] = Seq(`ocs-framework`)
lazy val githubReleases: Seq[ProjectReference]    = Seq.empty
lazy val unidocExclusions: Seq[ProjectReference]  = Seq.empty

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
  .aggregate(`ocs-framework`, `ocs-framework`)

lazy val `ocs-framework` = project
  .in(file("ocs/ocs-framework"))
  .settings(
    libraryDependencies ++= Dependencies.`ocs-framework`.value
  )

lazy val `ocs-framework-tests` = project
  .in(file("ocs/ocs-framework-tests"))
  .settings(
    libraryDependencies ++= Dependencies.`ocs-framework-tests`.value,
    Test / sourceDirectory := baseDirectory.value / "src" / "main"
  )
  .dependsOn(`ocs-framework`)

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)
