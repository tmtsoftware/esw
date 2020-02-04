package esw.agent.api

import java.nio.file.{Path, Paths}

import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.ConnectionType.AkkaType
import csw.location.api.models._
import csw.prefix.models.Prefix

sealed trait AgentCommand extends AgentAkkaSerializable

object AgentCommand {

  sealed trait SpawnCommand extends AgentCommand {
    def commandStrings(binariesPath: Path): List[String]
    val replyTo: ActorRef[SpawnResponse]
  }

  object SpawnCommand {
    def unapply(arg: SpawnCommand): Option[(ActorRef[SpawnResponse], ComponentId)] = arg match {
      case cmd: SpawnSelfRegistered     => Some((cmd.replyTo, cmd.componentId))
      case cmd: SpawnManuallyRegistered => Some((cmd.replyTo, cmd.registration.connection.componentId))
    }

    sealed trait SpawnSelfRegistered extends SpawnCommand {
      val componentId: ComponentId
      val connectionType: ConnectionType

      def connection: Connection = Connection.from(ConnectionInfo(componentId.prefix, componentId.componentType, connectionType))
    }

    object SpawnSelfRegistered {
      case class SpawnSequenceComponent(replyTo: ActorRef[SpawnResponse], prefix: Prefix) extends SpawnSelfRegistered {
        private val binaryName = "esw-ocs-app"

        override val componentId: ComponentId = ComponentId(prefix, SequenceComponent)

        override val connectionType: ConnectionType = AkkaType

        override def commandStrings(binariesPath: Path): List[String] = {
          val executablePath = Paths.get(binariesPath.toString, binaryName).toString
          List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
        }
      }
    }

    sealed trait SpawnManuallyRegistered extends SpawnCommand {
      val registration: Registration
    }

    object SpawnManuallyRegistered {
      case class SpawnRedis(replyTo: ActorRef[SpawnResponse], prefix: Prefix, port: Int, redisArguments: List[String])
          extends SpawnManuallyRegistered {

        override val registration: Registration = TcpRegistration(TcpConnection(ComponentId(prefix, ComponentType.Service)), port)

        private val binaryName = "redis-server"

        override def commandStrings(binariesPath: Path): List[String] = {
          val executablePath = Paths.get(binariesPath.toString, binaryName).toString
          (executablePath :: redisArguments) ++ List("--port", s"$port")
        }
      }
    }
  }

  private[agent] case class ProcessExited(componentId: ComponentId) extends AgentCommand

  case class KillComponent(replyTo: ActorRef[KillResponse], componentId: ComponentId) extends AgentCommand

  case class GetComponentStatus(replyTo: ActorRef[ComponentStatus], componentId: ComponentId) extends AgentCommand

  case class GetAgentStatus(replyTo: ActorRef[AgentStatus]) extends AgentCommand
}
