package esw.agent.pekko.client

import org.apache.pekko.actor.typed.ActorRef
import csw.location.api.models.ComponentType.{SequenceComponent, Service}
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.*
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.pekko.client.models.{ConfigFileLocation, ContainerConfig}
import esw.agent.service.api.*
import esw.agent.service.api.models.{KillResponse, SpawnContainersResponse, SpawnResponse}

import java.nio.file.Path
/*
 * These are messages(models) of agent actor.
 * These are being used in communication with the agent actor.
 */
sealed trait AgentCommand
sealed trait AgentRemoteCommand extends AgentCommand with AgentPekkoSerializable

object AgentCommand {

  sealed trait SpawnCommand extends AgentRemoteCommand {
    def replyTo: ActorRef[SpawnResponse]
    def commandArgs(extraArgs: List[String] = List.empty): List[String]
    def prefix: Prefix
    def connection: Connection

    def componentId: ComponentId = connection.componentId
  }

  object SpawnCommand {

    /**
     * This represents the message spawning SequenceComponent.
     * @param replyTo - Pekko Agent Actor
     * @param agentPrefix - the subsystem part of sequence component prefix.
     * @param componentName - the componentName part of sequence component prefix.
     * @param version - the version of sequencer script repo.
     * @param simulation - flag for starting SequenceComponent in simulation for testing purpose.
     */
    case class SpawnSequenceComponent(
        replyTo: ActorRef[SpawnResponse],
        agentPrefix: Prefix,
        componentName: String,
        version: Option[String],
        simulation: Boolean = false
    ) extends SpawnCommand {
      override val prefix: Prefix              = Prefix(agentPrefix.subsystem, componentName)
      override val connection: PekkoConnection = PekkoConnection(ComponentId(prefix, SequenceComponent))

      private val sim = if (simulation) List("--simulation") else List.empty
      override def commandArgs(extraArgs: List[String]): List[String] =
        List("seqcomp", "-s", prefix.subsystem.name, "-n", componentName) ++ extraArgs ++ sim
    }

    /**
     * This represents the message for Spawning SequenceManager.
     * @param replyTo - Pekko Agent Actor
     * @param obsModeConfigPath - Path for the obsModeConfig for the sequence manager.
     * @param isConfigLocal - flag which determines whether config file is local or in Configuration Service(remote).
     * @param version - version of sm (a ESW module) that will be started by agent.
     * @param simulation - flag for starting Sequence Manager in simulation for testing purpose.
     */
    case class SpawnSequenceManager(
        replyTo: ActorRef[SpawnResponse],
        obsModeConfigPath: Path,
        isConfigLocal: Boolean,
        version: Option[String],
        simulation: Boolean = false
    ) extends SpawnCommand {
      override val prefix: Prefix              = Prefix(ESW, "sequence_manager")
      override val connection: PekkoConnection = PekkoConnection(ComponentId(prefix, Service))
      private val command                      = List("start", "-o", obsModeConfigPath.toString)
      private val sim                          = if (simulation) List("--simulation") else List.empty

      override def commandArgs(extraArgs: List[String]): List[String] = {
        val args = if (isConfigLocal) command :+ "-l" else command
        args ++ extraArgs ++ sim
      }
    }

    /**
     * This represents the message for Spawning a container.
     * @param replyTo - Pekko Agent Actor
     * @param containerComponentId - ComponentId of the container to be spawned.
     * @param containerConfig [[esw.agent.pekko.client.models.ContainerConfig]]- Config for the container to be started.
     */
    case class SpawnContainer(
        replyTo: ActorRef[SpawnResponse],
        containerComponentId: ComponentId,
        containerConfig: ContainerConfig
    ) extends SpawnCommand {
      override val prefix: Prefix              = containerComponentId.prefix
      override val connection: PekkoConnection = PekkoConnection(containerComponentId)
      private val command                      = List(containerConfig.configFilePath.toString)

      override def commandArgs(extraArgs: List[String]): List[String] = {
        var args = command
        if (containerConfig.configFileLocation == ConfigFileLocation.Local) args = "--local" :: args
        args ++ extraArgs
      }
    }
  }

  /**
   * This represents the message for Spawning multiple containers.
   * @param replyTo - Pekko Agent Actor
   * @param hostConfigPath - Path for the host config that will be picked by agent to start multiple containers.
   * @param isConfigLocal - flag which determines whether config file is local or in Configuration Service(remote).
   */
  case class SpawnContainers(replyTo: ActorRef[SpawnContainersResponse], hostConfigPath: String, isConfigLocal: Boolean)
      extends AgentRemoteCommand

  /**
   * This represents the message for killing component that are registered in location service.
   * @param replyTo - Pekko Agent Actor
   * @param location [[csw.location.api.models.Location]] - location of the component to be killed.
   */
  case class KillComponent(replyTo: ActorRef[KillResponse], location: Location) extends AgentRemoteCommand
}
