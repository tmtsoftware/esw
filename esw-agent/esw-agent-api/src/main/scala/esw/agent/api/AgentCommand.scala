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
    def commandStrings(binariesPath: Path): List[String]
    val replyTo: ActorRef[Response]
    val componentId: ComponentId
    val connectionType: ConnectionType
  }

  private[agent] case class Finished(componentId: ComponentId) extends AgentCommand

  case class KillComponent(replyTo: ActorRef[Response], componentId: ComponentId) extends AgentCommand

  object SpawnCommand {

    def unapply(cmd: SpawnCommand): Option[(ActorRef[Response], ComponentId)] =
      Some((cmd.replyTo, cmd.componentId))

    case class SpawnSequenceComponent(replyTo: ActorRef[Response], prefix: Prefix) extends SpawnCommand {
      private val binaryName = "esw-ocs-app"

      override val componentId: ComponentId = ComponentId(prefix, SequenceComponent)

      override val connectionType: ConnectionType = AkkaType

      override def commandStrings(binariesPath: Path): List[String] = {
        val executablePath = Paths.get(binariesPath.toString, binaryName).toString
        List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
      }
    }
  }
}
