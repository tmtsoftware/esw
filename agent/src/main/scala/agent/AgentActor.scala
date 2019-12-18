package agent

import java.io.InputStream

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.scaladsl.{Framing, Keep, Source, StreamConverters}
import akka.util.ByteString
import csw.prefix.models.Subsystem

import scala.concurrent.Future

sealed trait AgentCommand {
  val strings: List[String]
}

case class SpawnSequenceComponent(subsystem: Subsystem, name: String) extends AgentCommand {
  private val executablePath = "/Users/dollygyanchandani/Projects/tmt/esw/target/universal/stage/bin/esw-ocs-app"
  override val strings       = List(executablePath, "seqcomp", "-s", subsystem.toString, "-n", name)
}

object AgentActor {

  def behavior: Behavior[AgentCommand] = Behaviors.receive { (ctx, command) =>
    import ctx.system
    runCommand(command)(system)
    Behaviors.same
  }

  private def printStreams(inputStreams: (() => InputStream)*)(implicit system: ActorSystem[_]): Future[Done] =
    inputStreams
      .foldLeft(Source.empty[ByteString])((source, inputStream) =>
        source.mergeMat(StreamConverters.fromInputStream(inputStream))(Keep.left)
      )
      .via(Framing.delimiter(ByteString("\n"), 256, allowTruncation = true))
      .map(_.utf8String)
      .runForeach(println)

  private def runCommand(agentCommand: AgentCommand)(implicit system: ActorSystem[_]): Unit = {
    val processBuilder = new ProcessBuilder(agentCommand.strings: _*)
    val process        = processBuilder.start()
    println("PID=" + process.pid())
    printStreams(process.getInputStream, process.getErrorStream)
  }
}
