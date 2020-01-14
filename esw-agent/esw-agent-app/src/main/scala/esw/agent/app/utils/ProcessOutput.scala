package esw.agent.app.utils

import java.io.InputStream

import esw.agent.app.utils.ProcessOutput.ConsoleWriter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class ProcessOutput(writer: ConsoleWriter = new ConsoleWriter()) {

  def attachToProcess(process: Process, processName: String): Unit = {
    def writeStream(stream: InputStream, writeStr: String => Unit): Unit =
      Source
        .fromInputStream(stream)
        .getLines()
        .foreach(str => writeStr(s"[$processName] $str"))

    Future {
      writeStream(process.getInputStream, writer.write)
      writeStream(process.getErrorStream, writer.writeErr)
    }
  }
}

object ProcessOutput {
  private[agent] class ConsoleWriter {
    def write(value: String): Unit    = println(value)
    def writeErr(value: String): Unit = Console.err.println(value)
  }
}
