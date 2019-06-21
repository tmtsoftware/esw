import sbt.{Def, _}

object Dependencies {
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
      AkkaHttp.`akka-http`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream-typed`,
      AkkaHttp.`akka-http-testkit`,
      Akka.`akka-stream-testkit`,
      Libs.scalatest
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
      Libs.scalatest,
      Csw.`csw-testkit`,
      Libs.`mockito-scala`,
      Akka.`akka-actor-testkit-typed`
    )
  )
}
