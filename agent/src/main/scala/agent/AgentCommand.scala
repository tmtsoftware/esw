package agent

import java.nio.file.Paths

import agent.Response.Done
import akka.actor.typed.ActorRef
import csw.prefix.models.Prefix

sealed trait AgentCommand

sealed trait ShellCommand extends AgentCommand {
  val strings: List[String]
  val prefix: Prefix
}
// todo: fix absolute path issue
object AgentCommand {
  case class SpawnSequenceComponent(replyTo: ActorRef[Response], prefix: Prefix) extends ShellCommand {
    private val executablePath: String = Paths.get("target/universal/stage/bin/esw-ocs-app").toAbsolutePath.toString
    override val strings               = List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
  }
  case class KillAllProcesses(replyTo: ActorRef[Done.type]) extends AgentCommand

  object SpawnSequenceComponent {
    def apply(prefix: Prefix)(replyTo: ActorRef[Response]): SpawnSequenceComponent = new SpawnSequenceComponent(replyTo, prefix)
  }
}
