package agent.utils

import java.io.InputStream

import agent.utils.ProcessOutput.{ConsoleWriter, ProcessTextLine}
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Framing, Source, StreamConverters}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{IOResult, OverflowStrategy}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.Failure

/**
 * This utility class provides ability to bind standard output and standard error channels
 * of a given process to respective channels of current process. It supports attaching multiple
 * processes outputs at runtime; all printing to to console concurrently. To ensure better
 * readability of console, it writes one line at a time atomically.
 */
class ProcessOutput(writer: ConsoleWriter = new ConsoleWriter())(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  //since this is lazy, the stream is only run
  //when first process is attached
  private lazy val channelActor = {
    val (ref, source) = ActorSource
      .actorRef[ProcessTextLine](
        PartialFunction.empty,
        PartialFunction.empty,
        1024,
        OverflowStrategy.dropHead
      )
      .preMaterialize()
    source
      .runForeach {
        case ProcessTextLine(text, processName, false) =>
          writer.write(s"[$processName] $text")
        case ProcessTextLine(text, processName, true) =>
          writer.writeErr(s"[$processName] $text")
      }
      .onComplete {
        case Failure(exception) => exception.printStackTrace()
        case _                  => //this stream has a hot source and will not finish
      }
    ref
  }

  private def convertToSource(
      inputStream: () => InputStream,
      err: Boolean,
      processName: String
  ): Source[ProcessTextLine, Future[IOResult]] =
    StreamConverters
      .fromInputStream(inputStream)
      .via(Framing.delimiter(ByteString("\n"), 1024, allowTruncation = true))
      .map(_.utf8String)
      .map(ProcessTextLine(_, processName, err))

  private def createSource(process: Process, processName: String): Source[ProcessTextLine, Future[IOResult]] = {
    val outSource = convertToSource(process.getInputStream _, err = false, processName)
    val errSource = convertToSource(process.getErrorStream _, err = true, processName)
    outSource merge errSource
  }

  /**
   * Attach stdout and stderr of given process to current process.
   * @param process process of which output needs to be attached
   * @param processName This is required to distinguish the output on console
   */
  def attachToProcess(process: Process, processName: String): Unit =
    createSource(process, processName).runForeach(channelActor ! _)
}

object ProcessOutput {

  private[agent] class ConsoleWriter {
    def write(value: String): Unit    = println(value)
    def writeErr(value: String): Unit = Console.err.println(value)
  }

  private[agent] case class ProcessTextLine(
      text: String,
      processName: String,
      err: Boolean = false
  )
}
