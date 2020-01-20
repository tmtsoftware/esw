package esw.agent.app.process

import java.io.InputStream

import akka.actor.typed.ActorSystem
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Framing, StreamConverters}
import akka.util.ByteString
import esw.agent.app.process.ProcessOutput.ConsoleWriter

class ProcessOutput(writer: ConsoleWriter = new ConsoleWriter())(implicit actorSystem: ActorSystem[_]) {

  private def writeStream(stream: () => InputStream, processName: String, writeStr: String => Unit): Unit =
    StreamConverters
      .fromInputStream(stream)
      .via(Framing.delimiter(ByteString("\n"), 1024, allowTruncation = true))
      .withAttributes(ActorAttributes.dispatcher("akka.actor.default-blocking-io-dispatcher"))
      .runForeach(str => writeStr(s"[$processName] ${str.utf8String}"))

  def attachToProcess(process: Process, processName: String): Unit = {
    writeStream(process.getInputStream _, processName, writer.write)
    writeStream(process.getErrorStream _, processName, writer.writeErr)
  }
}

object ProcessOutput {
  private[agent] class ConsoleWriter {
    def write(value: String): Unit    = println(value)
    def writeErr(value: String): Unit = Console.err.println(value)
  }
}
