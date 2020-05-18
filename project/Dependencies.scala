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
      Libs.`mockito-scala` % Test
    )
  )

  val OcsApiJvm: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      AkkaHttp.`akka-http`,
      Libs.`msocket-impl-jvm`,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val OcsHandler: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-aas-http`,
      AkkaHttp.`akka-http`,
      Libs.`msocket-impl-jvm`,
      Libs.scalatest.value         % Test,
      AkkaHttp.`akka-http-testkit` % Test,
      Akka.`akka-stream-testkit`   % Test,
      Libs.`mockito-scala`         % Test
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
      Libs.`msocket-impl-jvm`,
      Libs.blockhound,
      Libs.scalatest.value            % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`tmt-test-reporter`        % Test,
      Libs.`mockito-scala`            % Test
    )
  )

  val OcsApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-impl-jvm`,
      Libs.scalatest.value            % Test,
      Libs.`mockito-scala`            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val AgentApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Csw.`csw-location-client`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Libs.scalatest.value            % Test,
      Libs.`mockito-scala`            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val AgentClient: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Csw.`csw-prefix`.value,
      Csw.`csw-location-api`.value,
      Akka.`akka-actor-typed`,
      Libs.`mockito-scala`            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val EswHttpCore: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-commons`,
      Csw.`csw-network-utils`,
      Csw.`csw-location-client`,
      Csw.`csw-aas-http`,
      Csw.`csw-alarm-client`,
      Akka.`akka-actor-typed`,
      Csw.`csw-command-client`,
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-cors`,
      Csw.`csw-event-client`,
      Csw.`csw-params`.value,
      Csw.`csw-config-client`,
      Csw.`csw-time-scheduler`,
      Libs.`case-app`,
      Libs.`scala-async`,
      Libs.scalatest.value     % Test,
      Libs.`mockito-scala`     % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val IntegrationTest = Def.setting(
    Seq(
      Csw.`csw-logging-models`.value  % Test,
      Libs.scalatest.value            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Csw.`csw-integration-multi-jvm` % Test,
      Akka.`akka-multi-node-testkit`  % Test
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
      Libs.`mockito-scala`            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val OcsDslKt: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Kotlin.`stdlib-jdk8`,
      Kotlin.`coroutines-jdk8`,
      Kotlin.mockk             % Test,
      Kotlin.kotlintest        % Test,
      Libs.`jupiter-interface` % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswGatewayApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-api`.value,
      Csw.`csw-alarm-api`,
      Csw.`csw-command-api`.value,
      Csw.`csw-logging-models`.value,
      Csw.`csw-admin-api`.value,
      Csw.`csw-event-api`
    )
  )

  val EswGatewayImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-event-client`,
      Libs.caffeine,
      Csw.`csw-location-api`.value,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswGatewayServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-impl-jvm`,
      Csw.`csw-admin-impl`,
      Libs.`prometheus-akka-http`,
      Libs.`tmt-test-reporter`        % Test,
      Libs.`mockito-scala`            % Test,
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
      Libs.`mockito-scala`     % Test,
      Libs.scalatest.value     % Test,
      Libs.`tmt-test-reporter` % Test
    )
  )

  val EswSmApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Csw.`csw-location-api`.value,
      Libs.`mockito-scala` % Test,
      Libs.scalatest.value % Test
    )
  )

  val SmApiJvm: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      Akka.`akka-actor-typed`,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val EswSmApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.scalatest.value            % Test,
      Libs.`mockito-scala`            % Test,
      Libs.`tmt-test-reporter`        % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val EswCommons: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-location-api`.value,
      Akka.`akka-stream-typed`        % Provided,
      Akka.`akka-remote`              % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`mockito-scala`            % Test,
      Libs.scalatest.value            % Test,
      Libs.`tmt-test-reporter`        % Test
    )
  )

  val EswTestkit = Def.setting(
    Seq(
      Akka.`akka-actor-testkit-typed`,
      Csw.`csw-testkit`,
      Libs.scalatest.value,
      Libs.`tmt-test-reporter` % Test,
      Libs.`embedded-keycloak`
    )
  )
}
