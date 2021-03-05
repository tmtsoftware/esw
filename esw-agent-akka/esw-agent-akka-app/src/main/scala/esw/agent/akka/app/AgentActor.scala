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
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnContainer
import esw.agent.akka.client.AgentCommand._
import esw.agent.akka.client.models.ContainerConfig
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.constants.{AgentTimeouts, CommonTimeouts}

import java.nio.file.Path
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.chaining.scalaUtilChainingOps

class AgentActor(processManager: ProcessManager, configUtils: ConfigUtils, hostConfigPath: Option[Path], isConfigLocal: Boolean)(
    implicit
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
      hostConfigPath: Path,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] = {
    try {
      val hostConfig = getHostConfig(hostConfigPath, isConfigLocal)
      val spawnResponsesF = hostConfig.map { config =>
        val componentId = containerComponentId(config)
        componentId.prefix.toString() -> (agentRef ? (SpawnContainer(_, componentId, config)))(
          AgentTimeouts.SpawnComponent,
          system.scheduler
        )
      }.toMap
      Future
        .sequence(spawnResponsesF.values)
        .map(spawnResponses => Completed((spawnResponsesF.keys zip spawnResponses).toMap))
    }
    catch {
      case e: Exception => Future.successful(Failed(e.getMessage.tap(log.error(_))))
    }
  }

  private def getHostConfig(path: Path, isConfigLocal: Boolean): List[ContainerConfig] = {
    // FIXME: Create proper model for HostConfig, then no need to hand parse "containers" list
    val hostConfigF = configUtils
      .getConfig(path, isConfigLocal)
      .map(_.getConfigList("containers").asScala.map(ContainerConfig(_)).toList)
    // FIXME: Do not block inside actor
    Await.result(hostConfigF, CommonTimeouts.FetchConfig)
  }

  private def containerComponentId(config: ContainerConfig): ComponentId = {
    // FIXME: Do not block inside actor
    val containerConfig =
      Await.result(configUtils.getConfig(config.configFilePath, config.isConfigLocal), CommonTimeouts.FetchConfig)
    if (config.mode == "Standalone") {
      // FIXME: componentType is case insensitive, this will fail for 'HCD'
      val componentType = if (containerConfig.getString("componentType") == "hcd") ComponentType.HCD else ComponentType.Assembly
      ComponentId(Prefix(containerConfig.getString("prefix")), componentType)
    }
    else ComponentId(Prefix(Container, containerConfig.getString("name")), ComponentType.Container)
  }
}
