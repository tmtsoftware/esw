import sbt.{Def, _}

object Dependencies {
  val `ocs-framework`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Csw.`csw-params`.value, Akka.`akka-typed`, Libs.`scala-async`.value)
  )

  val `ocs-framework-tests`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Libs.scalatest)
  )

  val `gateway-server`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-command-client`,
      AkkaHttp.`akka-http`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Libs.`scopt`
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
