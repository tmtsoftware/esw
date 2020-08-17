package esw.agent.akka.client

import java.nio.file.Path

import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType.{SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnManuallyRegistered, SpawnSelfRegistered}
import esw.agent.service.api._
import esw.agent.service.api.models.ComponentStatus.{Initializing, Running}
import esw.agent.service.api.models.{AgentStatus, ComponentStatus, KillResponse, SpawnResponse}

sealed trait AgentCommand       extends AgentAkkaSerializable
sealed trait AgentRemoteCommand extends AgentCommand

object AgentCommand {

  sealed trait SpawnCommand extends AgentRemoteCommand {
    def replyTo: ActorRef[SpawnResponse]
    def commandArgs(extraArgs: List[String] = List.empty): List[String]
    def prefix: Prefix
    def connection: Connection

    def componentId: ComponentId = connection.componentId
    def isAutoRegistered: Boolean =
      this match {
        case _: SpawnSelfRegistered     => true
        case _: SpawnManuallyRegistered => false
      }
  }

  object SpawnCommand {
    sealed trait SpawnSelfRegistered extends SpawnCommand

    object SpawnSelfRegistered {
      case class SpawnSequenceComponent(
          replyTo: ActorRef[SpawnResponse],
          agentPrefix: Prefix,
          componentName: String,
          version: Option[String]
      ) extends SpawnSelfRegistered {
        override val prefix: Prefix             = Prefix(agentPrefix.subsystem, componentName)
        override val connection: AkkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
        override def commandArgs(extraArgs: List[String]): List[String] =
          List("seqcomp", "-s", prefix.subsystem.name, "-n", componentName) ++ extraArgs
      }

      case class SpawnSequenceManager(
          replyTo: ActorRef[SpawnResponse],
          obsModeConfigPath: Path,
          isConfigLocal: Boolean,
          version: Option[String]
      ) extends SpawnSelfRegistered {
        override val prefix: Prefix             = Prefix(ESW, "sequence_manager")
        override val connection: AkkaConnection = AkkaConnection(ComponentId(prefix, Service))
        private val command                     = List("start", "-o", obsModeConfigPath.toString)
        override def commandArgs(extraArgs: List[String]): List[String] = {
          val args = if (isConfigLocal) command :+ "-l" else command
          args ++ extraArgs
        }
      }
    }

    sealed trait SpawnManuallyRegistered extends SpawnCommand {
      def registration: Registration
    }

    object SpawnManuallyRegistered {
      case class SpawnRedis(replyTo: ActorRef[SpawnResponse], prefix: Prefix, port: Int, redisArguments: List[String])
          extends SpawnManuallyRegistered {
        override val connection: TcpConnection                          = TcpConnection(ComponentId(prefix, ComponentType.Service))
        override val registration: Registration                         = TcpRegistration(connection, port)
        override def commandArgs(extraArgs: List[String]): List[String] = redisArguments ++ List("--port", s"$port") ++ extraArgs
      }
    }
  }

  case class KillComponent(replyTo: ActorRef[KillResponse], location: Location)                            extends AgentRemoteCommand
  case class GetComponentStatus(replyTo: ActorRef[ComponentStatus], componentId: ComponentId)              extends AgentRemoteCommand
  case class GetAgentStatus(replyTo: ActorRef[AgentStatus])                                                extends AgentRemoteCommand
  private[agent] case class ProcessExited(componentId: ComponentId)                                        extends AgentCommand
  private[agent] case class UpdateComponentState(componentId: ComponentId, componentState: ComponentState) extends AgentCommand
}

case class ComponentState(process: Option[Process]) {
  def status: ComponentStatus = process.fold[ComponentStatus](Initializing)(_ => Running)
}
