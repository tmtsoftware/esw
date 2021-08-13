package esw.agent.akka.app

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.config.client.commons.ConfigUtils
import csw.location.api.models.{ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.Container
import esw.agent.akka.app.process.ProcessManager
import esw.agent.akka.client.AgentCommand
import esw.agent.akka.client.AgentCommand.*
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnContainer
import esw.agent.akka.client.models.*
import esw.agent.service.api.models.*
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.constants.AgentTimeouts

import java.nio.file.{Path, Paths}
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

class AgentActor(
    processManager: ProcessManager,
    configUtils: ConfigUtils,
    hostConfigPath: Option[String],
    isConfigLocal: Boolean
)(implicit
    system: ActorSystem[_],
    log: Logger
) {
  import system.executionContext

  private[agent] def behavior: Behavior[AgentCommand] = {
    Behaviors.setup { ctx =>
      hostConfigPath.foreach(p => ctx.self ! SpawnContainers(ctx.system.deadLetters, p, isConfigLocal))
      Behaviors.receiveMessage[AgentCommand] { command =>
        command match {
          case cmd: SpawnCommand                             => processManager.spawn(cmd).mapToAdt(_ => Spawned, Failed).map(cmd.replyTo ! _)
          case SpawnContainers(replyTo, path, isConfigLocal) => spawnContainers(ctx.self, path, isConfigLocal).map(replyTo ! _)
          case KillComponent(replyTo, location)              => processManager.kill(location).map(replyTo ! _)
        }
        Behaviors.same
      }
    }
  }

  private def spawnContainers(
      agentRef: ActorRef[AgentCommand],
      hostConfigPath: String,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] = {
    val hostConfigF = getHostConfig(Paths.get(hostConfigPath), isConfigLocal)
    hostConfigF
      .flatMap(hostConfig => {
        val spawnResponseMapF = spawnResponseMap(agentRef, hostConfig)
        spawnResponseMapF.flatMap(spawnResponseMap => {
          Future
            .sequence(spawnResponseMap.values)
            .map(spawnResponses => Completed((spawnResponseMap.keys zip spawnResponses).toMap))
        })
      })
      .recoverWith { case e: Exception =>
        Future.successful(Failed(e.getMessage.tap(log.error(_))))
      }
  }

  private def spawnResponseMap(
      agentRef: ActorRef[AgentCommand],
      hostConfig: HostConfig
  ): Future[Map[String, Future[SpawnResponse]]] = {
    val spawnResponsePairs = hostConfig.containers.map { config =>
      val componentIdF = getContainerComponentId(config)
      componentIdF.map(componentId => {
        componentId.prefix.toString() -> (agentRef ? (SpawnContainer(_, componentId, config)))(
          AgentTimeouts.SpawnComponent,
          system.scheduler
        )
      })
    }
    Future.sequence(spawnResponsePairs).map(_.toMap)
  }

  private def getHostConfig(path: Path, isConfigLocal: Boolean): Future[HostConfig] =
    configUtils.getConfig(path, isConfigLocal).map(HostConfig(_))

  private def getContainerComponentId(containerConfig: ContainerConfig): Future[ComponentId] = {
    val isContainerConfigLocal = containerConfig.configFileLocation == ConfigFileLocation.Local
    val containerInfoConfigF   = configUtils.getConfig(containerConfig.configFilePath, isContainerConfigLocal)
    containerInfoConfigF.map(containerInfoConfig => {
      if (containerConfig.mode == ContainerMode.Standalone) {
        val componentInfo = ComponentInfo(containerInfoConfig)
        ComponentId(componentInfo.prefix, componentInfo.componentType)
      }
      else {
        val containerInfo = ContainerInfo(containerInfoConfig)
        ComponentId(Prefix(Container, containerInfo.name), ComponentType.Container)
      }
    })
  }
}
