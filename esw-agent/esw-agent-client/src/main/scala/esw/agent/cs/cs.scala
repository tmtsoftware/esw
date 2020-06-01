package esw.agent.cs

object cs {
  private lazy val csGlobalSettings = CsGlobalSettings()
  import csGlobalSettings._

  private def csNative     = "cs"
  private def csLauncher   = getClass.getResource("/coursier").getPath
  def csInstalled: Boolean = new ProcessBuilder("command", "-v", csNative).start().waitFor() == 0

  private lazy val cs: String = if (csInstalled) csNative else csLauncher

  case class Launch(csSettings: CsSettings) {
    private val app      = s"${csSettings.name}:${csSettings.version}"
    private val javaOpts = csSettings.javaOpts.flatMap(List("-J", _))

    def launch(args: String*): List[String] =
      List(cs, "launch") ++ javaOpts ++ List("--channel", channel, app, "--") ++ args
  }

  def ocsApp: Launch = Launch(csGlobalSettings.ocsApp)
}
