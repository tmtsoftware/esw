import sbt.{Def, _}

object Dependencies {

  val OcsApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Csw.`csw-location-models`.value,
      Csw.`csw-database`,
      Libs.`scala-java8-compat`,
      Libs.`msocket-api`.value,
      Libs.scalatest       % Test,
      Libs.`mockito-scala` % Test
    )
  )

  val OcsImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
      Libs.enumeratum.value,
      Libs.`msocket-impl-jvm`,
      Libs.scalatest                  % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`mockito-scala`            % Test
    )
  )

  val OcsApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-impl-jvm`,
      Libs.scalatest                  % Test,
      Libs.`mockito-scala`            % Test,
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
      AkkaHttp.`akka-http-cors`,
      Csw.`csw-event-client`,
      Csw.`csw-params`.value,
      Csw.`csw-config-client`,
      Csw.`csw-time-scheduler`,
      Libs.`case-app`,
      Libs.`scala-async`,
      Libs.scalatest                  % Test,
      Csw.`csw-testkit`               % Test,
      Libs.`mockito-scala`            % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      AkkaHttp.`akka-http-testkit`    % Test,
      Akka.`akka-stream-testkit`      % Test
    )
  )

  val IntegrationTest = Def.setting(
    Seq(
      Csw.`csw-location-server-multi-jvm`,
      Csw.`csw-location-server-tests`,
      Libs.`scala-java8-compat` % Test,
      Csw.`csw-testkit`         % Test,
      Csw.`csw-admin-server`    % Test,
      Csw.`csw-logging-models`  % Test,
      Libs.scalatest            % Test,
      Akka.`akka-multi-node-testkit`
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
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val OcsDslKt: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Kotlin.`stdlib-jdk8`,
      Kotlin.`coroutines-jdk8`,
      Kotlin.mockk             % Test,
      Kotlin.kotlintest        % Test,
      Libs.`jupiter-interface` % Test
    )
  )

  val EswGatewayApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-api`.value,
      Csw.`csw-alarm-api`,
      Csw.`csw-command-api`,
      Csw.`csw-event-api`
    )
  )

  val EswGatewayImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-event-client`,
      Libs.`caffeine`
    )
  )

  val EswGatewayServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-impl-jvm`
    )
  )

  val EswSM: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      Libs.scalatest % Test
    )
  )
}
