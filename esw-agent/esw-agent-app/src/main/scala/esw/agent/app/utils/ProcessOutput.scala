package esw.agent.app.utils

import java.io.InputStream

import esw.agent.app.utils.ProcessOutput.ConsoleWriter

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.io.Source

class ProcessOutput(writer: ConsoleWriter = new ConsoleWriter())(implicit ec: ExecutionContext) {

  private def writeStream(stream: InputStream, processName: String, writeStr: String => Unit): Unit =
    Future {
      blocking {
        Source
          .fromInputStream(stream)
          .getLines()
          .foreach(str => writeStr(s"[$processName] $str"))
      }
    }

  def attachToProcess(process: Process, processName: String): Unit = {
    writeStream(process.getInputStream, processName, writer.write)
    writeStream(process.getErrorStream, processName, writer.writeErr)
  }
}

object ProcessOutput {
  private[agent] class ConsoleWriter {
    def write(value: String): Unit    = println(value)
    def writeErr(value: String): Unit = Console.err.println(value)
  }
}
