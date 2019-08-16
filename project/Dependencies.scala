import sbt.{Def, _}

object Dependencies {

  val OcsApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Csw.`csw-params`.value,
      Csw.`csw-location-api`,
      Csw.`csw-command-client`,
      Libs.`enumeratum`.value,
      Libs.scalatest                  % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`mockito-scala`            % Test
    )
  )

  val OcsImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Csw.`csw-location-client`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
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
      Libs.scalatest                  % Test,
      Libs.`mockito-scala`            % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val GatewayServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      Libs.`case-app`,
      Libs.scalatest       % Test,
      Libs.`mockito-scala` % Test
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
      Libs.`scopt`,
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
      Csw.`csw-testkit` % Test,
      Libs.scalatest    % Test
    )
  )

  val Utils: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-location-client`,
      Libs.scalatest                  % Test,
      Libs.`mockito-scala`            % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val MsocketCore: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Akka.`akka-stream`,
      AkkaHttp.`akka-http`,
      Borer.`borer-core`,
      Borer.`borer-derivation`,
      Borer.`borer-compat-akka`
    )
  )
}
