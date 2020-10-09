import sbt._

object Settings {
  def addAliases(): Seq[Setting[_]] = {
    addCommandAlias(
      "testAll",
      ";test; multi-jvm:test"
    ) ++
    addCommandAlias(
      "buildAll",
      ";scalafmtCheck; clean; makeSite; test:compile; multi-jvm:compile"
    ) ++
    addCommandAlias(
      "compileAll",
      "; scalafmtCheck; test:compile; multi-jvm:compile"
    ) ++
    addCommandAlias(
      "validateSite",
      ";makeSite; docs/Paradox/paradoxValidateLinks;"
    )
  }
}
