import sbt.{Def, _}

object Dependencies {

  val OcsApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Csw.`csw-location-models`.value,
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
      Libs.scalatest                  % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`mockito-scala`            % Test
    )
  )

  val OcsMacros: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Libs.`scala-reflect`, Libs.scalatest % Test)
  )

  val OcsApp: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`case-app`,
      Libs.`msocket-impl-jvm`,
      AkkaHttp.`akka-http-cors`,
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
      Csw.`csw-event-client`,
      Csw.`csw-params`.value,
      Csw.`csw-config-client`,
      Csw.`csw-time-scheduler`,
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
      Csw.`csw-testkit`        % Test,
      Csw.`csw-admin-server`   % Test,
      Csw.`csw-logging-models` % Test,
      Libs.scalatest           % Test,
      Libs.examples            % Test
    )
  )

  val Utils: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-location-client`,
      Csw.`csw-event-client`,
      Csw.`csw-command-client`,
      Csw.`csw-time-scheduler`,
      Csw.`csw-command-client`,
      Libs.`mockito-scala`            % Test,
      Akka.`akka-actor-testkit-typed` % Test
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
      Csw.`csw-event-client`
    )
  )

  val EswGatewayServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.`msocket-impl-jvm`,
      Libs.`case-app`
    )
  )

}
