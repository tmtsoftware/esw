import Common._
import org.tmt.sbt.docs.{Settings => DocSettings}
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

inThisBuild(
  CommonSettings
)

val KotlincOptions = Seq(
  "-opt-in=kotlin.time.ExperimentalTime",
  "-Xallow-any-scripts-in-source-roots",
  "-Xuse-fir-lt=false",
  "-jvm-target",
  "17"
)

lazy val aggregateProjects: Seq[ProjectReference] = Seq(
  `esw-ocs`,
  `esw-ocs-handler`,
  `esw-http-core`,
  `esw-gateway`,
  `esw-agent-pekko`,
  `esw-agent-service`,
  `esw-contract`,
  examples,
  `esw-constants`,
  `esw-commons`,
  `esw-test-commons`,
  `esw-sm`,
  `esw-testkit`,
  `esw-backend-testkit`,
  `esw-shell`,
  `esw-integration-test`,
  `esw-http-template-wiring`,
  `esw-services`,
  `esw-performance-test`
)

lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `esw-integration-test`,
  `esw-ocs-api`.js,
  `esw-gateway-api`.js,
  `esw-ocs-handler`,
  `esw-agent-service-api`.js,
  `esw-sm-api`.js,
  examples,
  `esw-shell`
)

lazy val esw = (project in file("."))
  .aggregate(aggregateProjects: _*)
  .enablePlugins(NoPublish, UnidocSitePlugin, GithubPublishPlugin, GitBranchPrompt, GithubRelease, ContractPlugin)
  .settings(DocSettings.makeSiteMappings(docs))
  .settings(Settings.addAliases())
  .settings(DocSettings.docExclusions(unidocExclusions))
  .settings(GithubRelease.githubReleases)
  .settings(
    generateContract := ContractPlugin.generate(`esw-contract`).value
  )
  .settings(
    ghreleaseRepoOrg  := "tmtsoftware",
    ghreleaseRepoName := EswKeys.projectName
  )

lazy val `esw-ocs` = project
  .in(file("esw-ocs"))
  .aggregate(
    `esw-ocs-api`.js,
    `esw-ocs-api`.jvm,
    `esw-ocs-dsl`,
    `esw-ocs-dsl-kt`,
    `esw-ocs-script-server`,
    `esw-ocs-impl`,
    `esw-ocs-app`
  )

lazy val `esw-ocs-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("esw-ocs/esw-ocs-api"))
  .jvmConfigure(
    _.enablePlugins(MaybeCoverage)
      .settings(libraryDependencies ++= Dependencies.OcsApiJvm.value)
      .dependsOn(`esw-constants`, `esw-test-commons` % Test)
  )
  //  the following setting is required by IntelliJ which could not handle cross-compiled Pekko types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.OcsApi.value
  )
  .jsSettings(jsTestArg)

lazy val `esw-ocs-handler` = project
  .in(file("esw-ocs/esw-ocs-handler"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsHandler.value
  )
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-commons`,
    `esw-test-commons` % Test
  )

lazy val `esw-ocs-impl` = project
  .in(file("esw-ocs/esw-ocs-impl"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsImpl.value,
    Test / fork := true,
    javaOptions += "-XX:+AllowRedefinitionToAddDeleteMethods"
  )
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-commons`,
    `esw-test-commons` % Test
  )

lazy val `esw-ocs-dsl` = project
  .in(file("esw-ocs/esw-ocs-dsl"))
  .enablePlugins(MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.OcsDsl.value)
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-ocs-impl`,
    `esw-test-commons` % Test
  )

lazy val `esw-ocs-dsl-kt` = project
  .in(file("esw-ocs/esw-ocs-dsl-kt"))
  .enablePlugins(KotlinPlugin, MaybeCoverage)
  .settings(
    Test / fork   := true, // fixme: temp fix to run test sequentially, otherwise LoopTest fails because of timings
    kotlinVersion := EswKeys.kotlinVersion,
    kotlincOptions ++= KotlincOptions
  )
  .settings(libraryDependencies ++= Dependencies.OcsDslKt.value)
  .dependsOn(`esw-ocs-dsl`)

lazy val `esw-ocs-script-server` = project
  .in(file("esw-ocs/esw-ocs-script-server"))
  .enablePlugins(KotlinPlugin)
  .settings(
    kotlinVersion := EswKeys.kotlinVersion,
    kotlincOptions ++= KotlincOptions
  )
  .settings(libraryDependencies ++= Dependencies.OcsScriptServer.value)
  .dependsOn(`esw-ocs-dsl`, `esw-ocs-impl`, `esw-ocs-app`, `esw-commons`)

lazy val `esw-ocs-app` = project
  .in(file("esw-ocs/esw-ocs-app"))
  .enablePlugins(EswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.OcsApp.value
  )
  .dependsOn(
    `esw-ocs-handler`,
    `esw-http-core`,
    `esw-ocs-impl`,
    `esw-agent-pekko-app`,
    `esw-test-commons` % Test
  )

lazy val `esw-agent-pekko` = project
  .in(file("esw-agent-pekko"))
  .aggregate(`esw-agent-pekko-client`, `esw-agent-pekko-app`)

lazy val `esw-agent-pekko-client` = project
  .in(file("esw-agent-pekko/esw-agent-pekko-client"))
  .enablePlugins(MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AgentPekkoClient.value)
  .dependsOn(`esw-constants`, `esw-agent-service-api`.jvm, `esw-test-commons` % Test)

lazy val `esw-agent-pekko-app` = project
  .in(file("esw-agent-pekko/esw-agent-pekko-app"))
  .enablePlugins(EswBuildInfo, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AgentPekkoApp.value)
  .dependsOn(
    `esw-agent-pekko-client`,
    `esw-commons`,
    `esw-test-commons` % Test
  )

lazy val `esw-agent-service` = project
  .in(file("esw-agent-service"))
  .aggregate(
    `esw-agent-service-api`.jvm,
    `esw-agent-service-impl`,
    `esw-agent-service-app`
  )

lazy val `esw-agent-service-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("esw-agent-service/esw-agent-service-api"))
  .jvmSettings(libraryDependencies ++= Dependencies.AgentServiceApiJvm.value)
  .jvmConfigure(
    _.enablePlugins(MaybeCoverage)
      .dependsOn(`esw-commons`, `esw-test-commons` % Test)
  )
  .settings(libraryDependencies ++= Dependencies.AgentServiceApi.value)

lazy val `esw-agent-service-impl` = project
  .in(file("esw-agent-service/esw-agent-service-impl"))
  .enablePlugins(MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AgentServiceImpl.value)
  .dependsOn(`esw-agent-service-api`.jvm, `esw-agent-pekko-client`, `esw-test-commons` % Test)

lazy val `esw-agent-service-app` = project
  .in(file("esw-agent-service/esw-agent-service-app"))
  .enablePlugins(EswBuildInfo, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AgentServiceApp.value)
  .dependsOn(`esw-agent-service-impl`, `esw-http-core`, `esw-commons`, `esw-test-commons` % Test)

lazy val `esw-http-core` = project
  .in(file("esw-http-core"))
  .enablePlugins(MaybeCoverage, EswBuildInfo)
  .settings(libraryDependencies ++= Dependencies.EswHttpCore.value)

lazy val `esw-integration-test` = project
  .in(file("esw-integration-test"))
  .settings(libraryDependencies ++= Dependencies.IntegrationTest.value)
  .settings(Test / fork := true)
  .enablePlugins(AutoMultiJvm)
  .dependsOn(
    `esw-gateway-server`,
    `esw-http-core`,
    `esw-ocs-impl`,
    `esw-ocs-script-server`, // needed for when tests run script server in the same process
    examples,
    `esw-ocs-app`,
    `esw-agent-pekko-app`,
    `esw-agent-pekko-client`,
    `esw-sm-app`,
    `esw-testkit`,
    `esw-agent-service-app`,
    `esw-shell`,
    `esw-test-commons` % Test
  )
  .settings(
    Test / test := {
      if (!sys.props.contains("disableIntegrationTests")) {
        sLog.value.info("============== Running publishLocal ==============")
        val _ = publishLocal.all(ScopeFilter(inAggregates(LocalRootProject))).value
        (Test / test).value
      }
      else {
        sLog.value.warn("============== Skipping integration tests ============== ")
      }
    }
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
  .jvmConfigure(_.enablePlugins(MaybeCoverage).settings(libraryDependencies += (Libs.`tmt-test-reporter` % Test)))
  //  the following setting is required by IntelliJ which could not handle cross-compiled Pekko types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayApi.value
  )
  .dependsOn(`esw-ocs-api`)
  .jsSettings(jsTestArg)

lazy val `esw-gateway-impl` = project
  .in(file("esw-gateway/esw-gateway-impl"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayImpl.value
  )
  .dependsOn(`esw-commons`, `esw-gateway-api`.jvm, `esw-test-commons` % Test)

lazy val `esw-gateway-server` = project
  .in(file("esw-gateway/esw-gateway-server"))
  .enablePlugins(EswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.EswGatewayServer.value
  )
  .dependsOn(
    `esw-gateway-impl`,
    `esw-ocs-handler`,
    `esw-ocs-impl`,
    `esw-http-core`,
    `esw-commons`,
    `esw-test-commons` % Test
  )

lazy val `esw-contract` = project
  .in(file("esw-contract"))
  .settings(libraryDependencies ++= Dependencies.EswContract.value)
  .dependsOn(
    `esw-ocs-api`.jvm,
    `esw-gateway-api`.jvm,
    `esw-sm-api`.jvm,
    `esw-agent-service-api`.jvm
  )

/* ================= Paradox Docs ============== */
lazy val docs = project
  .enablePlugins(NoPublish, ParadoxMaterialSitePlugin)
  .settings(paradoxProperties ++= Map("extref.esw_ocs_eng_ui.base_url" -> ESW_OCS_ENG_UI.baseUrl.value))

lazy val examples = project
  .in(file("examples"))
  .enablePlugins(KotlinPlugin)
  .settings(
    kotlinVersion := EswKeys.kotlinVersion,
    kotlincOptions ++= KotlincOptions
  )
  .dependsOn(`esw-ocs-dsl-kt`, `esw-ocs-app`)

lazy val `esw-sm` = project
  .aggregate(
    `esw-sm-api`.js,
    `esw-sm-api`.jvm,
    `esw-sm-impl`,
    `esw-sm-app`,
    `esw-sm-handler`
  )

lazy val `esw-sm-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("esw-sm/esw-sm-api"))
  .jvmConfigure(
    _.enablePlugins(MaybeCoverage)
      .settings(libraryDependencies ++= Dependencies.SmApiJvm.value)
      .dependsOn(`esw-constants`, `esw-commons`, `esw-test-commons` % Test, `esw-ocs-api`.jvm)
  )
  //  the following setting is required by IntelliJ which could not handle cross-compiled Pekko types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .settings(fork := false)
  .settings(
    libraryDependencies ++= Dependencies.EswSmApi.value
  )
  .jsSettings(jsTestArg)
  .dependsOn(`esw-ocs-api`)

lazy val `esw-sm-impl` = project
  .in(file("esw-sm/esw-sm-impl"))
  .enablePlugins(MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EswSmImpl.value)
  .dependsOn(`esw-sm-api`.jvm, `esw-ocs-api`.jvm, `esw-agent-pekko-client`, `esw-commons`, `esw-test-commons` % Test)

lazy val `esw-sm-handler` = project
  .in(file("esw-sm/esw-sm-handler"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.EswSmHandlers.value
  )
  .dependsOn(`esw-sm-api`.jvm, `esw-commons`, `esw-test-commons` % Test)

lazy val `esw-sm-app` = project
  .in(file("esw-sm/esw-sm-app"))
  .enablePlugins(EswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.EswSmApp.value
  )
  .dependsOn(
    `esw-sm-impl`,
    `esw-http-core`,
    `esw-sm-handler`
  )

lazy val `esw-services` = project
  .in(file("esw-services"))
  .enablePlugins(EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.EswServices.value
  )
  .dependsOn(
    `esw-commons`,
    `esw-agent-pekko-app`,
    `esw-agent-service-app`,
    `esw-gateway-server`,
    `esw-sm-app`
  )

lazy val `esw-constants` = project
  .in(file("esw-constants"))

lazy val `esw-commons` = project
  .in(file("esw-commons"))
  .enablePlugins(MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EswCommons.value)
  .dependsOn(`esw-test-commons` % Test)

lazy val `esw-test-commons` = project
  .in(file("esw-test-commons"))
  .settings(libraryDependencies ++= Dependencies.EswTestCommons.value)

lazy val `esw-testkit` = project
  .in(file("esw-testkit"))
  .settings(
    libraryDependencies ++= Dependencies.EswTestkit.value
  )
  .dependsOn(
    `esw-gateway-server`,
    `esw-ocs-app`,
    `esw-ocs-script-server`, // needed for when tests run script server in the same process
    `esw-agent-pekko-app`,
    `esw-sm-app`,
    `esw-agent-service-app`
  )

lazy val `esw-backend-testkit` = project
  .in(file("esw-backend-testkit"))
  .dependsOn(`esw-testkit`)
  .settings(
    libraryDependencies ++= Dependencies.BackendTestkit.value
  )

lazy val `esw-shell` = project
  .in(file("esw-shell"))
  .enablePlugins(EswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.EswShell.value,
    fork := false // this is needed for the ammonite shell to run
  )
  .dependsOn(`esw-ocs-impl`, `esw-sm-api`.jvm, `esw-agent-pekko-client`, `esw-gateway-impl`, `esw-testkit`)

lazy val `esw-http-template-wiring` = project
  .dependsOn(`esw-constants`, `esw-http-core`)
  .settings(
    libraryDependencies ++= Dependencies.Template.value
  )

lazy val `esw-performance-test` = project
  .dependsOn(
    `esw-testkit`,
    `esw-sm-api`.jvm,
    `esw-gateway-api`.jvm,
    `esw-ocs-dsl-kt`,
    `esw-ocs-app`
  )
  .enablePlugins(KotlinPlugin)
  .settings(
    libraryDependencies ++= Dependencies.PerformanceTest.value,
    kotlinVersion := EswKeys.kotlinVersion,
    kotlincOptions ++= KotlincOptions
  )
