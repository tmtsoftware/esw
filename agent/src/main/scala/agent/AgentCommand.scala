package agent

import java.nio.file.Paths

import akka.actor.typed.ActorRef
import csw.prefix.models.Prefix

sealed trait AgentCommand

object AgentCommand {
  private[agent] case object KillAllProcesses extends AgentCommand

  private[agent] case class ProcessRegistered(pid: Long, replyTo: ActorRef[Response]) extends AgentCommand

  private[agent] case class ProcessRegistrationFailed(pid: Long, replyTo: ActorRef[Response]) extends AgentCommand

  sealed trait SpawnCommand extends AgentCommand {
    val strings: List[String]
    val prefix: Prefix
  }

  object SpawnCommand {
    // todo: fix absolute path issue
    case class SpawnSequenceComponent(replyTo: ActorRef[Response], prefix: Prefix) extends SpawnCommand {
      private val executablePath: String = Paths.get("target/universal/stage/bin/esw-ocs-app").toAbsolutePath.toString
      override val strings               = List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
    }

    object SpawnSequenceComponent {
      def apply(prefix: Prefix)(replyTo: ActorRef[Response]): SpawnSequenceComponent = new SpawnSequenceComponent(replyTo, prefix)
    }
  }
}
