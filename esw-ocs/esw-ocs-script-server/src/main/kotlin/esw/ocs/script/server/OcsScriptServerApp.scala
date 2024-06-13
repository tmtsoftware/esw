package esw.ocs.script.server

import csw.prefix.models.Prefix
import esw.http.core.BuildInfo

object OcsScriptServerApp extends App {

  private case class Options(subsystem: String = "", name: String = "", script: String = "")

  private val parser = new scopt.OptionParser[Options]("ocs-script-server") {
    head("ocs-script-server", BuildInfo.version)

    opt[String]("subsystem").required() valueName "<subsystem>" action { (x, c) =>
      c.copy(subsystem = x)
    } text s"The subsystem for the sequencer"

    opt[String]("name").required() valueName "<name>" action { (x, c) =>
      c.copy(name = x)
    } text s"The name for the sequencer"
  }

  // Parse the command line options
  parser.parse(args, Options()) match {
    case Some(options) =>
      try {
        val prefix = Prefix(s"${options.subsystem}.${options.name}")
        run(prefix)
      }
      catch {
        case e: Throwable =>
          e.printStackTrace()
          System.exit(1)
      }
    case None => System.exit(1)
  }

  // Run the application
  private def run(prefix: Prefix): Unit = {
    val wiring = OcsScriptServerWiring(prefix)
    wiring.server.start()
  }
}
