import sbt.{Def, _}

object Dependencies {

  val OcsApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Csw.`csw-command-api`.value,
      Csw.`csw-location-api`.value,
      Csw.`csw-database`,
      Libs.`scala-java8-compat`,
      Libs.`msocket-api`.value,
      Libs.scalatest.value % Test,
      Libs.`mockito`       % Test
    )
  )

  val OcsApiJvm: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      AkkaHttp.`akka-http`,
      Libs.`msocket-http`,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val OcsHandler: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-aas-http`,
      AkkaHttp.`akka-http`,
      Libs.`msocket-http`,
      Libs.scalatest.value         % Test,
      AkkaHttp.`akka-http-testkit` % Test,
      Akka.`akka-stream-testkit`   % Test,
      Libs.`mockito`               % Test
    )
  )

  val OcsImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      Csw.`csw-event-client`,
      Csw.`csw-alarm-client`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
      Libs.enumeratum.value,
      Libs.`msocket-http`,
      Libs.blockhound,
      Libs.scalatest.value            % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`tmt-test-reporter`        % Test,
      Libs.`mockito`                  % Test
    )
  )

  val OcsApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Libs.`msocket-http`,
      Libs.scalatest.value            % Test,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val AgentServiceApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Csw.`csw-location-api`.value,
      Libs.`msocket-api`.value
    )
  )

  val AgentServiceApiJvm: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Libs.`msocket-http`)
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
      Libs.`msocket-http`,
      Libs.scalatest.value         % Test,
      Libs.`mockito`               % Test,
      Libs.`tmt-test-reporter`     % Test,
      AkkaHttp.`akka-http-testkit` % Test
    )
  )

  val AgentAkkaApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Csw.`csw-location-client`,
      Csw.`csw-config-client`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Libs.scalatest.value            % Test,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val AgentAkkaClient: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-prefix`.value,
      Akka.`akka-actor-typed`,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val EswHttpCore: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-commons`,
      Csw.`csw-network-utils`,
      Csw.`csw-location-client`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-cors`,
      Libs.`scala-async`,
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
      Akka.`akka-multi-node-testkit` % Test,
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
      Akka.`akka-actor-testkit-typed` % Test
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

  val OcsScriptKt: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Kotlin.`kotlin-scripting-jvm`,
    )
  )

  val OcsScriptHostKt: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Kotlin.`kotlin-scripting-common`,
      Kotlin.`kotlin-scripting-jvm`,
      Kotlin.`kotlin-scripting-jvm-host`,
//      Kotlin.`kotlin-scripting-dependencies`
    )
  )

  val EswGatewayApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-api`.value,
      Csw.`csw-alarm-api`,
      Csw.`csw-command-api`.value,
      Csw.`csw-command-client`,
      Csw.`csw-logging-models`.value,
      Csw.`csw-event-api`
    )
  )

  val EswGatewayImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Csw.`csw-event-client`,
      Libs.caffeine,
      Csw.`csw-location-api`.value,
      Libs.`mockito`                  % Test,
      Libs.scalatest.value            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-remote`              % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val EswGatewayServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      Csw.`csw-command-client`,
      Csw.`csw-config-client`,
      Libs.`case-app`,
      Libs.`msocket-http`,
      Libs.`tmt-test-reporter`        % Test,
      Libs.`mockito`                  % Test,
      Libs.scalatest.value            % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      AkkaHttp.`akka-http-testkit`    % Test,
      Akka.`akka-stream-testkit`      % Test
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
      Akka.`akka-actor-typed`,
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
      Akka.`akka-actor-typed`,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val EswSmApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Libs.scalatest.value            % Test,
      Libs.`mockito`                  % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val EswSmHandlers: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-http`,
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-testkit` % Test,
      Akka.`akka-stream-testkit`   % Test
    )
  )

  val EswCommons: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-config-client`,
      Csw.`csw-aas-http`,
      Csw.`csw-location-api`.value,
      Libs.`embedded-keycloak`,
      Akka.`akka-stream-typed`        % Provided,
      Libs.`case-app`                 % Provided,
      Akka.`akka-remote`              % Test,
      Akka.`akka-actor-testkit-typed` % Test,
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
      Akka.`akka-actor-testkit-typed`,
      Akka.`akka-remote`,
      Csw.`csw-prefix`.value,
      Libs.scalatest.value,
      Libs.`mockito`,
      Libs.`tmt-test-reporter`
    )
  )

  val EswTestkit = Def.setting(
    Seq(
      Akka.`akka-actor-testkit-typed`,
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
