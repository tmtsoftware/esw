package app.http

import app.http.ServerAppCommand.StartCommand
import caseapp.core.RemainingArgs
import csw.location.api.models.Metadata
import template.TemplateApp

// todo: not getting unregistered when kill control+C.
object ServerApp extends TemplateApp[ServerAppCommand] {
  override def run(command: ServerAppCommand, remainingArgs: RemainingArgs): Unit =
    command match {
      case StartCommand(port, prefix) =>
        val wiring = new ServerWiring(port, prefix)
        start(wiring, Metadata.empty)
    }
}
