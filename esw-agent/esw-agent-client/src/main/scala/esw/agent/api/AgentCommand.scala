package esw.agent.api

import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models._
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.SpawnCommand.{SpawnManuallyRegistered, SpawnSelfRegistered}
import esw.agent.api.ComponentStatus.{Initializing, Running}

sealed trait AgentCommand       extends AgentAkkaSerializable
sealed trait AgentRemoteCommand extends AgentCommand

object AgentCommand {

  sealed trait SpawnCommand extends AgentRemoteCommand {
    def replyTo: ActorRef[SpawnResponse]
    def commandArgs: List[String]
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
      case class SpawnSequenceComponent(replyTo: ActorRef[SpawnResponse], prefix: Prefix, version: Option[String] = None)
          extends SpawnSelfRegistered {
        override val connection: AkkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
        override val commandArgs: List[String]  = List("seqcomp", "-s", prefix.subsystem.name, "-n", prefix.componentName)
      }
    }

    sealed trait SpawnManuallyRegistered extends SpawnCommand {
      def registration: Registration
    }

    object SpawnManuallyRegistered {
      case class SpawnRedis(replyTo: ActorRef[SpawnResponse], prefix: Prefix, port: Int, redisArguments: List[String])
          extends SpawnManuallyRegistered {
        override val connection: TcpConnection  = TcpConnection(ComponentId(prefix, ComponentType.Service))
        override val registration: Registration = TcpRegistration(connection, port)
        override val commandArgs: List[String]  = redisArguments ++ List("--port", s"$port")
      }
    }
  }

  case class KillComponent(replyTo: ActorRef[KillResponse], componentId: ComponentId)                      extends AgentRemoteCommand
  case class GetComponentStatus(replyTo: ActorRef[ComponentStatus], componentId: ComponentId)              extends AgentRemoteCommand
  case class GetAgentStatus(replyTo: ActorRef[AgentStatus])                                                extends AgentRemoteCommand
  private[agent] case class ProcessExited(componentId: ComponentId)                                        extends AgentCommand
  private[agent] case class UpdateComponentState(componentId: ComponentId, componentState: ComponentState) extends AgentCommand
}

case class ComponentState(process: Option[Process]) {
  def status: ComponentStatus = process.fold[ComponentStatus](Initializing)(_ => Running)
}
