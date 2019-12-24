package agent

import java.nio.file.Paths

import akka.actor.typed.ActorRef
import csw.prefix.models.Prefix

sealed trait AgentCommand {
  val strings: List[String]
  val prefix: Prefix
}
// todo: fix absolute path issue
object AgentCommand {
  case class SpawnSequenceComponent(replyTo: ActorRef[Response], prefix: Prefix) extends AgentCommand {
    private val executablePath: String = Paths.get("target/universal/stage/bin/esw-ocs-app").toAbsolutePath.toString
    override val strings               = List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
  }
  object SpawnSequenceComponent {
    def apply(prefix: Prefix)(replyTo: ActorRef[Response]): SpawnSequenceComponent = new SpawnSequenceComponent(replyTo, prefix)
  }
}
