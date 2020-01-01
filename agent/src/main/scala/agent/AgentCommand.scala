package agent

import java.nio.file.{Path, Paths}

import akka.actor.typed.ActorRef
import csw.prefix.models.Prefix

sealed trait AgentCommand

object AgentCommand {
  private[agent] case object KillAllProcesses                                                 extends AgentCommand
  private[agent] case class ProcessRegistered(pid: Long, replyTo: ActorRef[Response])         extends AgentCommand
  private[agent] case class ProcessRegistrationFailed(pid: Long, replyTo: ActorRef[Response]) extends AgentCommand

  sealed trait SpawnCommand extends AgentCommand {
    def strings(binariesPath: Path): List[String]
    val binaryName: String
    val prefix: Prefix
  }

  object SpawnCommand {
    case class SpawnSequenceComponent(replyTo: ActorRef[Response], prefix: Prefix) extends SpawnCommand {
      val binaryName = "esw-ocs-app"
      override def strings(binariesPath: Path): List[String] = {
        val executablePath = Paths.get(binariesPath.toString, "esw-ocs-app").toString
        List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
      }
    }

    object SpawnSequenceComponent {
      def apply(prefix: Prefix)(replyTo: ActorRef[Response]): SpawnSequenceComponent = new SpawnSequenceComponent(replyTo, prefix)
    }
  }
}
