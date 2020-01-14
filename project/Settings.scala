import sbt._

object Settings {
  def addAliases(): Seq[Setting[_]] = {
    addCommandAlias(
      "testAll",
      ";test; multi-jvm:test"
    ) ++
    addCommandAlias(
      "buildAll",
      ";set every enableFatalWarnings := true; scalafmtCheck; clean; makeSite; test:compile; multi-jvm:compile; set every enableFatalWarnings := false"
    ) ++
    addCommandAlias(
      "compileAll",
      ";set every enableFatalWarnings := true; scalafmtCheck; test:compile; multi-jvm:compile; set every enableFatalWarnings := false;"
    ) ++
    addCommandAlias(
      "openSite",
      "docs/Paradox/paradoxBrowse"
    )
  }
}
