package esw.http.template.wiring

import caseapp.Command
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import csw.location.api.models.Metadata
import esw.constants.CommonTimeouts

import scala.concurrent.Await
import scala.util.control.NonFatal

trait ServerApp extends CommandsEntryPoint {
  def appName: String
  def appVersion: String

  abstract class Runner[T: {Parser, Help}] extends Command[T] {
    def start(wiring: ServerWiring, metadata: Metadata): Unit = {
      try {
        wiring.actorRuntime.startLogging(progName, appVersion)
        wiring.logger.debug(s"starting $appName")
        val (binding, _) = Await.result(wiring.start(metadata), CommonTimeouts.Wiring)
        wiring.logger.info(s"$appName online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
      }
      catch {
        case NonFatal(ex) =>
          ex.printStackTrace()
          wiring.logger.error(s"$appName crashed")
          exit(1)
      }
    }
  }
}
