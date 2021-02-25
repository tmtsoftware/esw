package esw.agent.akka.app

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.config.client.commons.ConfigUtils
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.Container
import esw.agent.akka.app.process.ProcessManager
import esw.agent.akka.client.AgentCommand
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnContainer
import esw.agent.akka.client.AgentCommand._
import esw.agent.akka.client.models.ContainerConfig
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.constants.{AgentTimeouts, CommonTimeouts}

import java.nio.file.Path
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class AgentActor(processManager: ProcessManager, configUtils: ConfigUtils, hostConfigPath: Option[Path], isConfigLocal: Boolean)(
    implicit system: ActorSystem[_]
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
      hostConfigPath: Path,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] = {
    val hostConfig = getHostConfig(hostConfigPath, isConfigLocal)
    val spawnFuturesMap = hostConfig
      .map(c => {
        val componentId = containerComponentId(c)
        componentId.prefix.toString() -> (agentRef ? (SpawnContainer(_, componentId, c)))(
          AgentTimeouts.SpawnComponent,
          system.scheduler
        )
      })
      .toMap
    Future.sequence(spawnFuturesMap.values).map(fs => SpawnContainersResponse((spawnFuturesMap.keys zip fs).toMap))
  }

  private def getHostConfig(path: Path, isConfigLocal: Boolean): List[ContainerConfig] = {
    val containerConfigsF = configUtils
      .getConfig(path, isConfigLocal)
      .map(config => {
        config.getConfigList("containers").asScala.map(c => ContainerConfig(c)).toList
      })
    Await.result(containerConfigsF, CommonTimeouts.FetchConfig)
  }

  private def containerComponentId(config: ContainerConfig): ComponentId = {
    val containerConfig =
      Await.result(configUtils.getConfig(config.configFilePath, config.isConfigLocal), CommonTimeouts.FetchConfig)
    if (config.mode == "Standalone") {
      val componentType = if (containerConfig.getString("componentType") == "hcd") ComponentType.HCD else ComponentType.Assembly
      ComponentId(Prefix(containerConfig.getString("prefix")), componentType)
    }
    else ComponentId(Prefix(Container, containerConfig.getString("name")), ComponentType.Container)
  }
}
