import sbt.{Def, _}

object Dependencies {
  val `ocs-framework`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Csw.`csw-params`.value, Akka.`akka-typed`, Libs.`scala-async`)
  )

  val `ocs-framework-tests`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Libs.scalatest)
  )

  val `gateway-server`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Akka.`akka-actor`,
      Akka.`akka-stream`
    )
  )

  val `gateway-server-tests`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      AkkaHttp.`akka-http-testkit`,
      Akka.`akka-stream-testkit`,
      Libs.scalatest
    )
  )

  val `http-server`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-network-utils`,
      Csw.`csw-location-client`,
      Akka.`akka-actor`,
      Libs.`scopt`,
      Libs.`scala-async`
    )
  )

  val `http-server-tests`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.scalatest,
      Csw.`csw-testkit`
    )
  )
}
