package esw.agent.api

import java.nio.file.{Path, Paths}

import akka.actor.typed.ActorRef
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.ConnectionType.AkkaType
import csw.location.models.{ComponentId, ConnectionType}
import csw.prefix.models.Prefix

sealed trait AgentCommand extends AgentAkkaSerializable

object AgentCommand {
  sealed trait SpawnCommand extends AgentCommand {
    def strings(binariesPath: Path): List[String]
    val prefix: Prefix
    val replyTo: ActorRef[Response]
    val componentId: ComponentId
    val connectionType: ConnectionType
  }

  case class Finished(spawnCommand: SpawnCommand) extends AgentCommand

  object SpawnCommand {
    case class SpawnSequenceComponent(replyTo: ActorRef[Response], prefix: Prefix) extends SpawnCommand {
      private val binaryName = "esw-ocs-app"

      override val componentId: ComponentId = ComponentId(prefix, SequenceComponent)

      override val connectionType: ConnectionType = AkkaType

      override def strings(binariesPath: Path): List[String] = {
        val executablePath = Paths.get(binariesPath.toString, binaryName).toString
        List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
      }
    }

    object SpawnSequenceComponent {
      def apply(prefix: Prefix)(replyTo: ActorRef[Response]): SpawnSequenceComponent = new SpawnSequenceComponent(replyTo, prefix)
    }
  }
}
