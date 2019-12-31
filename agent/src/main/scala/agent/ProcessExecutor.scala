package agent

import agent.AgentCommand.SpawnCommand
import agent.Response.Failed
import agent.utils.ProcessOutput

import scala.util.Try
import scala.util.control.NonFatal

class ProcessExecutor(output: ProcessOutput) {
  private val log = AgentLogger.getLogger
  import log._

  def runCommand(spawnCommand: SpawnCommand): Either[Failed, Long] =
    Try {
      val processBuilder = new ProcessBuilder(spawnCommand.strings: _*)
      debug(s"starting command", Map("command" -> processBuilder.command()))
      val process = processBuilder.start()
      output.attachProcess(process, spawnCommand.prefix.value)
      debug(s"new process spawned", Map("pid" -> process.pid()))
      process.pid()
    }.toEither.left.map {
      case NonFatal(err) =>
        error("command failed to run", map = Map("command" -> spawnCommand), ex = err)
        Failed(err.getMessage)
    }
}
