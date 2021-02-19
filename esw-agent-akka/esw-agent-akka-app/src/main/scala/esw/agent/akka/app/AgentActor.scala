package esw.agent.akka.app

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.config.client.commons.ConfigUtils
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

class AgentActor(processManager: ProcessManager, configUtils: ConfigUtils, hostConfigPath: Path, isConfigLocal: Boolean)(implicit
    system: ActorSystem[_]
) {
  import system.executionContext

  private[agent] def behavior: Behavior[AgentCommand] = {
    Behaviors.setup { ctx =>
      ctx.self ! StartContainers(ctx.system.deadLetters)
      Behaviors.receiveMessage[AgentCommand] { command =>
        command match {
          case cmd: SpawnCommand                => processManager.spawn(cmd).mapToAdt(_ => Spawned, Failed).map(cmd.replyTo ! _)
          case StartContainers(replyTo)         => spawnContainers(ctx.self).map(replyTo ! _)
          case KillComponent(replyTo, location) => processManager.kill(location).map(replyTo ! _)
        }
        Behaviors.same
      }
    }
  }

  private def spawnContainers(agentRef: ActorRef[AgentCommand]): Future[StartContainersResponse] = {
    val hostConfig = getHostConfig
    val spawnFutures = hostConfig.map(c => {
      (agentRef ? (SpawnContainer(_, c)))(
        AgentTimeouts.SpawnComponent,
        system.scheduler
      )
    })
    Future.sequence(spawnFutures).map(StartContainersResponse)
  }

  private def getHostConfig: List[ContainerConfig] = {
    val containerConfigsF = configUtils
      .getConfig(hostConfigPath, isConfigLocal)
      .map(config => {
        config.getConfigList("containers").asScala.map(c => ContainerConfig(c)).toList
      })
    Await.result(containerConfigsF, CommonTimeouts.Wiring)
  }
}
