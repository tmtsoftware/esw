package agent.app.utils

import agent.api.AgentCommand.SpawnCommand
import agent.api.Response.Failed
import agent.app.AgentSettings
import csw.logging.api.scaladsl.Logger

import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.util.Try
import scala.util.control.NonFatal

class ProcessExecutor(output: ProcessOutput, agentSettings: AgentSettings, logger: Logger) {
  import logger._

  def runCommand(spawnCommand: SpawnCommand): Either[Failed, Long] =
    Try {
      val processBuilder = new ProcessBuilder(spawnCommand.strings(agentSettings.binariesPath): _*)
      debug(s"starting command", Map("command" -> processBuilder.command()))
      val process = processBuilder.start()
      output.attachToProcess(process, spawnCommand.prefix.value)
      debug(s"new process spawned", Map("pid" -> process.pid()))
      process.pid()
    }.toEither.left.map {
      case NonFatal(err) =>
        error("command failed to run", map = Map("command" -> spawnCommand), ex = err)
        Failed(err.getMessage)
    }

  def killProcess(pid: Long): Boolean =
    ProcessHandle
      .of(pid)
      .map(p => p.destroyForcibly())
      .asScala
      .getOrElse(false)
}
