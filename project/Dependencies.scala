import sbt.{Def, _}

object Dependencies {

  val `ocs-framework-api`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Csw.`csw-params`.value,
      Csw.`csw-location-api`,
      Libs.`enumeratum`.value,
      Libs.scalatest % Test
    )
  )

  val `ocs-framework`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-params`.value,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
      Libs.scalatest                  % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val `async-macros`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Libs.`scala-reflect`, Libs.scalatest % Test)
  )

  val `gateway-server`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-commons`,
      AkkaHttp.`akka-http`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      AkkaHttp.`akka-http-testkit` % Test,
      Akka.`akka-stream-testkit`   % Test,
      Libs.scalatest               % Test,
      Libs.`mockito-scala`         % Test
    )
  )

  val `http-server`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-network-utils`,
      Csw.`csw-location-client`,
      Akka.`akka-actor-typed`,
      Csw.`csw-command-client`,
      Libs.`scopt`,
      Libs.`scala-async`,
      Libs.scalatest                  % Test,
      Csw.`csw-testkit`               % Test,
      Libs.`mockito-scala`            % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )
}
