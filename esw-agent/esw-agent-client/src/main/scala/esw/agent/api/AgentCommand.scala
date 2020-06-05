package esw.agent.api

import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models._
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.SpawnCommand.{SpawnManuallyRegistered, SpawnSelfRegistered}

sealed trait AgentCommand extends AgentAkkaSerializable

object AgentCommand {

  sealed trait SpawnCommand extends AgentCommand {
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
      case class SpawnSequenceComponent(
          replyTo: ActorRef[SpawnResponse],
          prefix: Prefix,
          version: Option[String] = None,
          javaOpts: List[String] = Nil
      ) extends SpawnSelfRegistered {
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

  case class KillComponent(replyTo: ActorRef[KillResponse], componentId: ComponentId)         extends AgentCommand
  case class GetComponentStatus(replyTo: ActorRef[ComponentStatus], componentId: ComponentId) extends AgentCommand
  case class GetAgentStatus(replyTo: ActorRef[AgentStatus])                                   extends AgentCommand
  private[agent] case class ProcessExited(componentId: ComponentId)                           extends AgentCommand
}
