import sbt._

object Dependencies {

  val OcsApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Csw.`csw-command-api`.value,
      Csw.`csw-location-api`.value,
      Csw.`csw-database`,
      MSocket.`msocket-api`.value,
      Libs.scalatest.value % Test,
      Libs.`mockito`       % Test
    )
  )

  val OcsApiJvm: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-stream`,
      MSocket.`msocket-http`,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val OcsHandler: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-aas-http`,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-stream`,
      MSocket.`msocket-http`,
      Libs.scalatest.value         % Test,
      PekkoHttp.`pekko-http-testkit` % Test,
      Pekko.`pekko-stream-testkit`   % Test,
      Libs.`mockito`               % Test
    )
  )

  val OcsImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      Csw.`csw-event-client`,
      Csw.`csw-alarm-client`,
      Pekko.`pekko-actor-typed`,
      Pekko.`pekko-stream-typed`,
      Libs.`dotty-cps-async`.value,
      Libs.enumeratum.value,
      MSocket.`msocket-http`,
      Libs.blockhound,
      Libs.scalatest.value            % Test,
      Pekko.`pekko-actor-testkit-typed` % Test,
      Libs.`tmt-test-reporter`        % Test,
      Libs.`mockito`                  % Test
    )
  )

  val OcsApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      MSocket.`msocket-http`,
      Libs.scalatest.value            % Test,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val AgentServiceApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Borer.`borer-compat-pekko`,
      Csw.`csw-location-api`.value,
      MSocket.`msocket-api`.value
    )
  )

  val AgentServiceApiJvm: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(MSocket.`msocket-http`)
  )

  val AgentServiceImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-location-client`,
      Libs.scalatest.value     % Test,
      Libs.`mockito`           % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val AgentServiceApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      MSocket.`msocket-http`,
      Libs.scalatest.value         % Test,
      Libs.`mockito`               % Test,
      Libs.`tmt-test-reporter`     % Test,
      PekkoHttp.`pekko-http-testkit` % Test
    )
  )

  val AgentPekkoApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Csw.`csw-location-client`,
      Csw.`csw-config-client`,
      Pekko.`pekko-actor-typed`,
      Pekko.`pekko-stream`,
      Libs.scalatest.value            % Test,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val AgentPekkoClient: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-prefix`.value,
      Pekko.`pekko-actor-typed`,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val EswHttpCore: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-commons`,
      Csw.`csw-network-utils`,
      Csw.`csw-location-client`,
      Pekko.`pekko-actor-typed`,
      PekkoHttp.`pekko-http`,
      Borer.`borer-compat-pekko`,
      Pekko.`pekko-stream`,
      PekkoHttp.`pekko-http-cors`,
      Libs.`dotty-cps-async`.value,
      Libs.scalatest.value     % Test,
      Libs.`mockito`           % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswWiring: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-network-utils`,
      Csw.`csw-alarm-client`,
      Csw.`csw-location-client`,
      Csw.`csw-command-client`,
      Csw.`csw-event-client`,
      Csw.`csw-params`.value,
      Csw.`csw-config-client`,
      Csw.`csw-time-scheduler`,
      Libs.scalatest.value     % Test,
      Libs.`mockito`           % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val IntegrationTest = Def.setting(
    Seq(
      Pekko.`pekko-multi-node-testkit` % Test,
      Csw.`csw-logging-client`       % Test,
      Csw.`csw-logging-models`.value % Test,
      Libs.scalatest.value           % Test,
      Libs.`tmt-test-reporter`       % Test
    )
  )

  val OcsDsl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-location-client`,
      Csw.`csw-event-client`,
      Csw.`csw-command-client`,
      Csw.`csw-time-scheduler`,
      Csw.`csw-command-client`,
      Csw.`csw-alarm-client`,
      Csw.`csw-config-client`,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val OcsDslKt: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Kotlin.`stdlib-jdk8`,
      Kotlin.`coroutines-core`,
      Kotlin.`coroutines-core-jvm`,
      Kotlin.`coroutines-jdk8`,
      Kotlin.mockk             % Test,
      Kotlin.kotlintest        % Test,
      Libs.`jupiter-interface` % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val OcsScriptServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Csw.`csw-command-client`,
      Csw.`csw-logging-models`.value,
      Csw.`csw-location-api`.value,
      Libs.`play-json`,
      Libs.scopt,
      Pekko.`pekko-actor-typed`,
      PekkoHttp.`pekko-http`,
      PekkoHttp.`pekko-http-cors`,
      PekkoHttp.`pekko-http-spray-json`,
      Kotlin.`stdlib-jdk8`,
      Kotlin.`coroutines-core`,
      Kotlin.`coroutines-core-jvm`,
      Kotlin.`coroutines-jdk8`,
      Http4k.`http4k-core`,
      Http4k.`http4k-server-jetty`,
      Kotlin.mockk             % Test,
      Kotlin.kotlintest        % Test,
      Libs.`jupiter-interface` % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswGatewayApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      MSocket.`msocket-api`.value,
      Csw.`csw-alarm-api`,
      Csw.`csw-command-api`.value,
      Csw.`csw-command-client`,
      Csw.`csw-logging-models`.value,
      Csw.`csw-event-api`
    )
  )

  val EswGatewayImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Pekko.`pekko-actor-typed`,
      Csw.`csw-event-client`,
      Libs.caffeine,
      Csw.`csw-location-api`.value,
      Libs.`mockito`                  % Test,
      Libs.scalatest.value            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-remote`              % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val EswGatewayServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      Csw.`csw-command-client`,
      Csw.`csw-config-client`,
      Libs.`case-app`,
      MSocket.`msocket-http`,
      Libs.`tmt-test-reporter`        % Test,
      Libs.`mockito`                  % Test,
      Libs.scalatest.value            % Test,
      Pekko.`pekko-actor-testkit-typed` % Test,
      PekkoHttp.`pekko-http-testkit`    % Test,
      Pekko.`pekko-stream-testkit`      % Test
    )
  )

  val EswContract: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-contract`,
      Csw.`csw-params`.value,
      Libs.`tmt-test-reporter` % Test,
      Libs.scalatest.value     % Test
    )
  )

  val EswSmImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-location-api`.value,
      Csw.`csw-config-client`,
      Pekko.`pekko-actor-typed`,
      Libs.`mockito`           % Test,
      Libs.scalatest.value     % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswSmApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Csw.`csw-location-api`.value,
      Libs.`mockito`       % Test,
      Libs.scalatest.value % Test
    )
  )

  val SmApiJvm: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Pekko.`pekko-actor-typed`,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val EswSmApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Libs.scalatest.value            % Test,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val EswSmHandlers: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      MSocket.`msocket-http`,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-stream`,
      PekkoHttp.`pekko-http-testkit` % Test,
      Pekko.`pekko-stream-testkit`   % Test
    )
  )

  val EswCommons: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-config-client`,
      Csw.`csw-aas-http`,
      Csw.`csw-location-api`.value,
      Libs.`embedded-keycloak`,
      Pekko.`pekko-stream-typed`        % Provided,
      Libs.`case-app`                 % Provided,
      Pekko.`pekko-remote`              % Test,
      Pekko.`pekko-actor-testkit-typed` % Test,
      Libs.`mockito`                  % Test,
      Libs.scalatest.value            % Test
    )
  )

  val EswServices: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Csw.`csw-services`,
      Libs.scalatest.value     % Test,
      Libs.`mockito`           % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswTestCommons: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Pekko.`pekko-actor-testkit-typed`,
      Pekko.`pekko-remote`,
      Csw.`csw-prefix`.value,
      Libs.scalatest.value,
      Libs.`mockito`,
      Libs.`tmt-test-reporter`
    )
  )

  val EswTestkit = Def.setting(
    Seq(
      Pekko.`pekko-actor-testkit-typed`,
      Csw.`csw-testkit`,
      Libs.scalatest.value,
      Libs.`embedded-keycloak`,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val BackendTestkit = Def.setting(
    Seq(
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswShell = Def.setting(
    Seq(
      Csw.`csw-framework`,
      Libs.`ammonite`,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val Template = Def.setting(
    Seq(
      Csw.`csw-aas-http`,
      Csw.`csw-alarm-api`,
      Csw.`csw-alarm-client`,
      Csw.`csw-command-api`.value,
      Csw.`csw-command-client`,
      Csw.`csw-config-client`,
      Csw.`csw-event-api`,
      Csw.`csw-event-client`,
      Csw.`csw-time-scheduler`,
      Libs.`case-app`,
      Csw.`csw-testkit`        % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val PerformanceTest = Def.setting(
    Seq(
      Libs.`hdr-histogram`,
      Libs.`tmt-test-reporter` % Test
    )
  )
}
