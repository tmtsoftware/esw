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
      "validateSite",
      ";makeSite; docs/Paradox/paradoxValidateLinks;"
    )
  }

  // export CSW_VERSION env variable which is compatible with esw
  def cswVersion: String = (sys.env ++ sys.props).get("CSW_VERSION") match {
    case Some(v) => v
    case None    => "0.1.0-SNAPSHOT"
  }
}
