package agent

import java.nio.file.Paths

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import csw.prefix.models.Prefix
import RichProcess._
import scala.util.control.NonFatal

sealed trait AgentCommand {
  val strings: List[String]
  val prefix: Prefix
}

case class SpawnSequenceComponent(prefix: Prefix) extends AgentCommand {
  private val executablePath: String =
    Paths.get("target/universal/stage/bin/esw-ocs-app").toAbsolutePath.toString
  override val strings = List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
}

object AgentActor {

  def behavior: Behavior[AgentCommand] = Behaviors.receive { (ctx, command) =>
    import ctx.system
    runCommand(command)(system)
    Behaviors.same
  }

  private def runCommand(agentCommand: AgentCommand)(implicit system: ActorSystem[_]): Unit = {
    try {
      val processBuilder = new ProcessBuilder(agentCommand.strings: _*)
      val process        = processBuilder.start()
      println("PID=" + process.pid())
      process.attachToConsole(agentCommand.prefix)
    }
    catch {
      case NonFatal(err) => err.printStackTrace()
    }
  }
}
