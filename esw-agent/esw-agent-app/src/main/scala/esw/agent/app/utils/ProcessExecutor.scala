package esw.agent.app.utils

import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.Response.Failed
import esw.agent.app.AgentSettings

import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.util.Try
import scala.util.control.NonFatal

// $COVERAGE-OFF$
class ProcessExecutor(output: ProcessOutput, agentSettings: AgentSettings, logger: Logger) {
  import logger._

  def runCommand(command: List[String], prefix: Prefix): Either[Failed, Long] =
    Try {
      val processBuilder = new ProcessBuilder(command: _*)
      debug(s"starting command", Map("command" -> processBuilder.command()))
      val process = processBuilder.start()
      output.attachToProcess(process, prefix.toString.toLowerCase)
      debug(s"new process spawned", Map("pid" -> process.pid()))
      process.pid()
    }.toEither.left.map {
      case NonFatal(err) =>
        error("command failed to run", map = Map("command" -> command, "prefix" -> prefix.toString.toLowerCase), ex = err)
        Failed(err.getMessage)
    }

  def killProcess(pid: Long): Boolean =
    ProcessHandle
      .of(pid)
      .map(p => p.destroyForcibly())
      .asScala
      .getOrElse(false)
}
// $COVERAGE-ON$
