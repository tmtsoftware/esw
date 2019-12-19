package agent

import java.io.InputStream

import akka.actor.typed.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{Framing, Source, StreamConverters}
import akka.util.ByteString
import csw.prefix.models.Prefix

import scala.concurrent.Future
import scala.util.Failure

object RichProcessExt {
  case class ProcessTextLine(text: String, prefix: Prefix, err: Boolean = false) {
    def print(): Unit = {
      val pFunction: (Any => Unit) = if (err) Console.err.println else println
      pFunction(s"[${prefix.value}] $text")
    }
  }

  implicit class RichProcess(process: Process) {
    private def convertToSource(
        inputStream: () => InputStream,
        err: Boolean,
        prefix: Prefix
    ): Source[ProcessTextLine, Future[IOResult]] =
      StreamConverters
        .fromInputStream(inputStream)
        .via(Framing.delimiter(ByteString("\n"), 256, allowTruncation = true))
        .map(_.utf8String)
        .map(ProcessTextLine(_, prefix, err))

    private def source(prefix: Prefix): Source[ProcessTextLine, Future[IOResult]] = {
      val s1 = convertToSource(process.getInputStream _, err = false, prefix)
      val s2 = convertToSource(process.getErrorStream _, err = true, prefix)
      s1 merge s2
    }

    def attachToConsole(prefix: Prefix)(implicit actorSystem: ActorSystem[_]): Unit = {
      import actorSystem.executionContext
      source(prefix)
        .runForeach(_.print())
        .onComplete {
          case Failure(exception) => exception.printStackTrace()
          case _                  =>
        }
    }
  }
}
