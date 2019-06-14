import sbt.{Def, _}

object Dependencies {
  val `ocs-framework`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq.empty
  )

  val `ocs-framework-tests`: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(Libs.scalatest)
  )
}
