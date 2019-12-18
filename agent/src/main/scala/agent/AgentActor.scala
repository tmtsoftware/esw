package agent

import java.io.InputStream

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.IOResult
import akka.stream.scaladsl.{Framing, Keep, Source, StreamConverters}
import akka.util.ByteString
import csw.prefix.models.Subsystem

import scala.concurrent.Future

sealed trait AgentMsg
case class SpawnSequenceComponent(subsystem: Subsystem, compName: String) extends AgentMsg {
  def executablePath = "/Users/dollygyanchandani/Projects/tmt/esw/target/universal/stage/bin/esw-ocs-app"
}

object AgentActor {

  def runCommand(command: String*)(implicit system: ActorSystem[_]): Unit = {
    val processBuilder = new ProcessBuilder(command: _*)
    val process = processBuilder.start()
    println("PID=" + process.pid())

    def printStreams(is: (() => InputStream)*) = {
      val value: Source[ByteString, NotUsed] =
        is.foldLeft(Source.empty[ByteString])((a, b) => {
          val value1: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(b)
          a.mergeMat(value1)(Keep.left)
        })
      value
        .via(Framing.delimiter(ByteString("\n"), 256, allowTruncation = true))
        .map(_.utf8String)
        .runForeach(println)
    }

    printStreams(process.getInputStream, process.getErrorStream)
  }

  def behavior: Behavior[AgentMsg] = Behaviors.receive { (ctx, msg) =>
    import ctx.system
    msg match {
      case c @ SpawnSequenceComponent(ss, cN) =>
        runCommand(c.executablePath, "seqcomp", "-s", ss.toString, "-n", cN)(system)
        Behaviors.same
    }
  }
}
