package template

import caseapp.core.app.CommandApp
import csw.location.api.models.Metadata

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

// todo: not getting unregistered when kill control+C.
trait TemplateApp[T] extends CommandApp[T] {

  def start(wiring: TemplateWiring, metadata: Metadata): Unit = {
    try {
      wiring.actorRuntime.startLogging(progName, appVersion)
      wiring.logger.debug(s"starting $appName")
      Await.result(wiring.start(metadata), 10.seconds)
      wiring.logger.info(s"$appName started") // add host and port
    }
    catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
        wiring.logger.error(s"$appName crashed")
        exit(1)
    }
  }
}
