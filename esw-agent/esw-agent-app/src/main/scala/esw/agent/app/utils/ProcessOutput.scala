package esw.agent.app.utils

import esw.agent.app.utils.ProcessOutput.ConsoleWriter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class ProcessOutput(writer: ConsoleWriter = new ConsoleWriter()) {
  def attachToProcess(process: Process, processName: String): Unit = {
    Future {
      Source
        .fromInputStream(process.getInputStream)
        .getLines()
        .foreach(str => writer.write(s"[$processName] $str"))

      Source
        .fromInputStream(process.getErrorStream)
        .getLines()
        .foreach(str => writer.writeErr(s"[$processName] $str"))
    }
  }
}

object ProcessOutput {
  private[agent] class ConsoleWriter {
    def write(value: String): Unit    = println(value)
    def writeErr(value: String): Unit = Console.err.println(value)
  }
}
