package esw.agent.app.process

import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.app.AgentSettings

import scala.util.Try
import scala.util.control.NonFatal

// $COVERAGE-OFF$
class ProcessExecutor(output: ProcessOutput, agentSettings: AgentSettings, logger: Logger) {
  import logger._

  def runCommand(command: List[String], prefix: Prefix): Either[String, Process] =
    Try {
      val processBuilder = new ProcessBuilder(command: _*)
      debug(s"starting command", Map("command" -> processBuilder.command()))
      val process = processBuilder.start()
      output.attachToProcess(process, prefix.toString)
      debug(s"new process spawned", Map("pid" -> process.pid()))
      process
    }.toEither.left.map {
      case NonFatal(err) =>
        error("command failed to run", map = Map("command" -> command, "prefix" -> prefix.toString), ex = err)
        err.getMessage
    }
}
// $COVERAGE-ON$
