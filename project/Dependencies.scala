import sbt._

object Dependencies {
  val esw = Def.setting(
    Seq(Libs.scalatest)
  )
}