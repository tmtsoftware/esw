package esw.agent.akka.app.process

import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix

import scala.util.Try

// $COVERAGE-OFF$
class ProcessExecutor(output: ProcessOutput)(implicit log: Logger) {
  import log._

  //starts the process with the given list of command using ProcessBuilder
  //names the process with the given prefix
  //if process fails to start successfully it returns the error message
  //if process starts successfully it return process
  def runCommand(command: List[String], prefix: Prefix): Either[String, Process] =
    Try {
      println(s"Spawn command String : ${command.mkString(" ")}")
      val processBuilder = new ProcessBuilder(command: _*)
      debug(s"starting command", Map("command" -> processBuilder.command()))
      val process = processBuilder.start()
      output.attachToProcess(process, prefix.toString)
      debug(s"new process spawned", Map("pid" -> process.pid()))
      process
    }.toEither.left.map { err =>
      error("command failed to run", map = Map("command" -> command, "prefix" -> prefix.toString), ex = err)
      err.getMessage
    }
}
// $COVERAGE-ON$
