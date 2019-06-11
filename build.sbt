val aggregateProjects = Seq.empty

lazy val esw = (project in file("."))
  .aggregate(aggregateProjects: _*)
  .enablePlugins(NoPublish, UnidocSite, GithubPublishDocs, GitBranchPrompt, GithubRelease, CoursierPlugin)
  .disablePlugins(BintrayPlugin)
//  .settings(Settings.mergeSiteWith(docs))
//  .settings(Settings.addAliases)
//  .settings(Settings.docExclusions(unidocExclusions))
//  .settings(Settings.multiJvmTestTask(multiJvmProjects))
//  .settings(GithubRelease.githubReleases(githubReleases))
//  .settings(
//    bootstrap in Coursier := CoursierPlugin.bootstrapTask(githubReleases).value
//  )
