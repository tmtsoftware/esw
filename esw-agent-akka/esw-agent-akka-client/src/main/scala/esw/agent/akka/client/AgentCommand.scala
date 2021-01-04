package esw.agent.akka.client

import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType.{SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api._
import esw.agent.service.api.models.{KillResponse, SpawnResponse}

import java.nio.file.Path

sealed trait AgentCommand
sealed trait AgentRemoteCommand extends AgentCommand with AgentAkkaSerializable

object AgentCommand {

  sealed trait SpawnCommand extends AgentRemoteCommand {
    def replyTo: ActorRef[SpawnResponse]
    def commandArgs(extraArgs: List[String] = List.empty): List[String]
    def prefix: Prefix
    def connection: Connection

    def componentId: ComponentId = connection.componentId
  }

  object SpawnCommand {

    case class SpawnRedis(
        replyTo: ActorRef[SpawnResponse],
        prefix: Prefix,
        confPath: Path,
        port: Option[Int],
        version: Option[String]
    ) extends SpawnCommand {
      private val sentinel       = "redis-sentinel"
      private def sentinelRunCmd = s"$sentinel $confPath"

      override def commandArgs(extraArgs: List[String]): List[String] = {
        def prefixArgs = List("--prefix", prefix.toString)

        def sentinelArgs =
          port.fold(List("--command", sentinelRunCmd))(port =>
            List("--command", s"$sentinelRunCmd --port $port", "--port", port.toString)
          )

        prefixArgs ++ sentinelArgs ++ extraArgs
      }

      override def connection: Connection = TcpConnection(ComponentId(prefix, Service))
    }

    case class SpawnPostgres(
        replyTo: ActorRef[SpawnResponse],
        prefix: Prefix,
        pgDataConfPath: Path,
        port: Option[Int],
        dbUnixSocketDirs: String,
        version: Option[String]
    ) extends SpawnCommand {
      private val postgres       = "postgres"
      private def postgresRunCmd = s"$postgres --hba_file=$pgDataConfPath --unix_socket_directories=$dbUnixSocketDirs"

      override def commandArgs(extraArgs: List[String]): List[String] = {
        def prefixArgs = List("--prefix", prefix.toString)
        def postgresArgs =
          port.fold(List("--command", postgresRunCmd))(p => List("--command", s"$postgresRunCmd -i -p $p", "--port", p.toString))

        prefixArgs ++ postgresArgs ++ extraArgs
      }

      override def connection: Connection = TcpConnection(ComponentId(prefix, Service))
    }

    case class SpawnSequenceComponent(
        replyTo: ActorRef[SpawnResponse],
        agentPrefix: Prefix,
        componentName: String,
        version: Option[String]
    ) extends SpawnCommand {
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
    ) extends SpawnCommand {
      override val prefix: Prefix             = Prefix(ESW, "sequence_manager")
      override val connection: AkkaConnection = AkkaConnection(ComponentId(prefix, Service))
      private val command                     = List("start", "-o", obsModeConfigPath.toString)
      override def commandArgs(extraArgs: List[String]): List[String] = {
        val args = if (isConfigLocal) command :+ "-l" else command
        args ++ extraArgs
      }
    }
  }

  case class KillComponent(replyTo: ActorRef[KillResponse], location: Location) extends AgentRemoteCommand
}
