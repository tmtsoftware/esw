import sbt.{Def, _}

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

object ESW_OCS_ENG_UI {
  val baseUrl = Def.setting(s"https://tmtsoftware.github.io/esw-ocs-eng-ui/${readVersion("ESW_OCS_ENG_UI_VERSION")}/%s")

  private def readVersion(envVersionKey: String): String =
    (sys.env ++ sys.props).get(envVersionKey) match {
      case Some(v) => v
      case None    => "0.1.0-SNAPSHOT"
    }
}
