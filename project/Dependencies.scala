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
      Csw.`csw-command-client`,
      Csw.`csw-network-utils`,
      Csw.`csw-location-client`,
      AkkaHttp.`akka-http`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Libs.`scopt`,
      Libs.`scala-async`
    )
  )

  val `gateway-server-tests`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Libs.scalatest, 
      AkkaHttp.`akka-http-testkit`,
      Akka.`akka-stream-testkit`
    )
  )
}
