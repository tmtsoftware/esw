import sbt.{Def, _}

object Dependencies {

  val OcsFrameworkApi: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Csw.`csw-params`.value,
      Csw.`csw-location-api`,
      Libs.`enumeratum`.value,
      Libs.scalatest % Test
    )
  )

  val OcsFramework: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
      Libs.scalatest                  % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val AsyncMacros: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Libs.`scala-reflect`, Libs.scalatest % Test)
  )

  val GatewayServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      AkkaHttp.`akka-http-testkit` % Test,
      Akka.`akka-stream-testkit`   % Test,
      Libs.scalatest               % Test,
      Libs.`mockito-scala`         % Test
    )
  )

  val TemplateHttpServer: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-commons`,
      Csw.`csw-network-utils`,
      Csw.`csw-location-client`,
      Akka.`akka-actor-typed`,
      Csw.`csw-command-client`,
      Csw.`csw-event-client`,
      Libs.`scopt`,
      Libs.`scala-async`,
      Libs.scalatest                  % Test,
      Csw.`csw-testkit`               % Test,
      Libs.`mockito-scala`            % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )
}
