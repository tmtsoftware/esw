import sbt._

object Settings {
  def addAliases: Seq[Setting[_]] = {
    addCommandAlias(
      "testAll",
      "test; gradle clean test"
    ) ++
    addCommandAlias(
      "buildAll",
      ";set every enableFatalWarnings := true; scalafmtCheck; clean; makeSite; test:compile; set every enableFatalWarnings := false"
    )
  }
}
