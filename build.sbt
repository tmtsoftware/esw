import org.tmt.sbt.docs.{Settings => DocSettings}
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

lazy val aggregateProjects: Seq[ProjectReference] =
  Seq(
    `esw-ocs`,
    `esw-ocs-handler`,
    `esw-http-core`,
    `esw-gateway`,
    `esw-integration-test`,
    `esw-agent`,
    `esw-contract`,
    examples,
    `esw-commons`,
    `esw-sm`,
    `esw-testkit`
  )

lazy val githubReleases: Seq[ProjectReference] = Seq(`esw-ocs-app`, `esw-gateway-server`, `esw-sm-app`)
lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `esw-integration-test`,
  `esw-ocs-api`.js,
  `esw-gateway-api`.js,
  `esw-ocs-handler`,
  `esw-agent`,
  `esw-sm-api`.js,
  examples
)

val enableCoverage         = sys.props.get("enableCoverage").contains("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val esw = (project in file("."))
  .aggregate(aggregateProjects: _*)
  .enablePlugins(NoPublish, UnidocSitePlugin, GithubPublishPlugin, GitBranchPrompt, GithubRelease, ContractPlugin)
  .disablePlugins(BintrayPlugin)
  .settings(DocSettings.makeSiteMappings(docs))
  .settings(Settings.addAliases())
  .settings(DocSettings.docExclusions(unidocExclusions))
  .settings(GithubRelease.githubReleases(githubReleases))
  .settings(
    generateContract := ContractPlugin.generate(`esw-contract`).value
  )
lazy val `esw-ocs` = project
  .in(file("esw-ocs"))
  .aggregate(
    `esw-ocs-api`.js,
    `esw-ocs-api`.jvm,
    `esw-ocs-dsl`,
    `esw-ocs-dsl-kt`,
    `esw-ocs-impl`,
    `esw-ocs-app`
  )

lazy val `esw-ocs-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("esw-ocs/esw-ocs-api"))
  .jvmConfigure(
    _.enablePlugins(MaybeCoverage, PublishBintray)
      settings (libraryDependencies ++= Dependencies.OcsApiJvm.value)
  )
  //  the following setting is required by IntelliJ which could not handle cross-compiled Akka types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.OcsApi.value
  )

lazy val `esw-ocs-handler` = project
  .in(file("esw-ocs/esw-ocs-handler"))
  .enablePlugins(MaybeCoverage, PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.OcsHandler.value
  )
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-commons` % "test->test"
  )

lazy val `esw-ocs-impl` = project
  .in(file("esw-ocs/esw-ocs-impl"))
  .enablePlugins(MaybeCoverage, PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.OcsImpl.value
  )
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-commons` % "compile->compile;test->test"
  )

lazy val `esw-ocs-dsl` = project
  .in(file("esw-ocs/esw-ocs-dsl"))
  .settings(libraryDependencies ++= Dependencies.OcsDsl.value)
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-ocs-impl`,
    `esw-commons` % "test->test"
  )

lazy val `esw-ocs-dsl-kt` = project
  .in(file("esw-ocs/esw-ocs-dsl-kt"))
  .enablePlugins(KotlinPlugin)
  .settings(
    fork in Test := true, // fixme: temp fix to run test sequentially, otherwise LoopTest fails because of timings
    kotlinVersion := "1.3.61",
    kotlincOptions ++= Seq("-Xuse-experimental=kotlin.time.ExperimentalTime", "-jvm-target", "1.8")
  )
  .settings(libraryDependencies ++= Dependencies.OcsDslKt.value)
  .dependsOn(`esw-ocs-dsl`)

lazy val `esw-ocs-app` = project
  .in(file("esw-ocs/esw-ocs-app"))
  .enablePlugins(EswBuildInfo, DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsApp.value
  )
  .dependsOn(
    `esw-ocs-handler`,
    `esw-http-core`,
    `esw-ocs-impl`,
    `esw-commons` % "test->test"
  )

lazy val `esw-agent` = project
  .in(file("esw-agent"))
  .aggregate(
    `esw-agent-app`,
    `esw-agent-client`
  )

lazy val `esw-agent-app` = project
  .in(file("esw-agent/esw-agent-app"))
  .enablePlugins(EswBuildInfo, DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AgentApp.value)
  .dependsOn(
    `esw-agent-client`
  )

lazy val `esw-agent-client` = project
  .in(file("esw-agent/esw-agent-client"))
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AgentClient.value)

lazy val `esw-http-core` = project
  .in(file("esw-http-core"))
  .enablePlugins(PublishBintray, MaybeCoverage, EswBuildInfo)
  .settings(libraryDependencies ++= Dependencies.EswHttpCore.value)

lazy val `esw-integration-test` = project
  .in(file("esw-integration-test"))
  .settings(libraryDependencies ++= Dependencies.IntegrationTest.value)
  .settings(fork in Test := true)
  .enablePlugins(AutoMultiJvm)
  .dependsOn(
    `esw-gateway-server`,
    `esw-http-core`,
    `esw-ocs-impl`,
    examples,
    `esw-ocs-app`,
    `esw-agent-app`,
    `esw-agent-client`,
    `esw-sm-app`,
    `esw-testkit`
  )

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
  .jvmConfigure(_.settings(libraryDependencies += (Libs.`tmt-test-reporter` % Test)))
  //  the following setting is required by IntelliJ which could not handle cross-compiled Akka types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayApi.value
  )
  .dependsOn(`esw-ocs-api`)

lazy val `esw-gateway-impl` = project
  .in(file("esw-gateway/esw-gateway-impl"))
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayImpl.value
  )
  .dependsOn(`esw-gateway-api`.jvm)

lazy val `esw-gateway-server` = project
  .in(file("esw-gateway/esw-gateway-server"))
  .enablePlugins(EswBuildInfo, DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayServer.value
  )
  .dependsOn(
    `esw-gateway-impl`,
    `esw-ocs-handler`,
    `esw-ocs-impl`,
    `esw-http-core`,
    `esw-commons` % "test->test"
  )

lazy val `esw-contract` = project
  .in(file("esw-contract"))
  .settings(libraryDependencies ++= Dependencies.EswContract.value)
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-gateway-api`.jvm
  )

/* ================= Paradox Docs ============== */
lazy val docs = project
  .enablePlugins(NoPublish, ParadoxMaterialSitePlugin)

lazy val examples = project
  .in(file("examples"))
  .enablePlugins(KotlinPlugin)
  .settings(
    kotlinVersion := "1.3.61",
    kotlincOptions ++= Seq("-Xuse-experimental=kotlin.time.ExperimentalTime", "-jvm-target", "1.8")
  )
  .dependsOn(`esw-ocs-dsl-kt`)

lazy val `esw-sm` = project
  .aggregate(
    `esw-sm-api`.js,
    `esw-sm-api`.jvm,
    `esw-sm-impl`,
    `esw-sm-app`
  )

lazy val `esw-sm-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("esw-sm/esw-sm-api"))
  .jvmConfigure(
    _.enablePlugins(MaybeCoverage, PublishBintray)
      settings (libraryDependencies ++= Dependencies.SmApiJvm.value)
  )
  .dependsOn(`esw-ocs-api`)
  //  the following setting is required by IntelliJ which could not handle cross-compiled Akka types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.EswSmApi.value
  )

lazy val `esw-sm-impl` = project
  .in(file("esw-sm/esw-sm-impl"))
  .enablePlugins(MaybeCoverage, PublishBintray)
  .settings(libraryDependencies ++= Dependencies.EswSmImpl.value)
  .dependsOn(`esw-sm-api`.jvm, `esw-ocs-api`.jvm, `esw-agent-client`, `esw-commons` % "compile->compile;test->test")

lazy val `esw-sm-app` = project
  .in(file("esw-sm/esw-sm-app"))
  .enablePlugins(EswBuildInfo, DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.EswSmApp.value
  )
  .dependsOn(
    `esw-sm-impl`,
    `esw-http-core`
  )

lazy val `esw-commons` = project
  .in(file("esw-commons"))
  .settings(libraryDependencies ++= Dependencies.EswCommons.value)

lazy val `esw-testkit` = project
  .in(file("esw-testkit"))
  .settings(
    libraryDependencies ++= Dependencies.EswTestkit.value
  )
  .dependsOn(
    `esw-gateway-server`,
    `esw-ocs-app`,
    `esw-agent-app`
  )
