package esw.services.apps

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.services.utils.ColoredConsole.GREEN
import esw.agent.akka.app.AgentWiring
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.SpawnResponse
import esw.commons.utils.files.FileUtils
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.{Await, ExecutionContext}

class SequenceManager(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) {

  private val agentConfig: Config                  = ConfigFactory.load()
  private val smAgentPrefix: Prefix                = Prefix(ESW, "sm_machine")
  private val smAgent: ManagedService[AgentWiring] = Agent.service(enable = true, smAgentPrefix, agentConfig)
  implicit val executionContext: ExecutionContext  = actorSystem.executionContext

  def service(
      enable: Boolean,
      maybeObsModeConfigPath: Option[Path],
      simulation: Boolean
  ): ManagedService[SpawnResponse] =
    ManagedService(
      "sequence-manager",
      enable,
      () => startSM(getConfig(maybeObsModeConfigPath), simulation),
      _ => stopSM()
    )

  private def getConfig(maybeObsModeConfigPath: Option[Path]): Path =
    maybeObsModeConfigPath.getOrElse {
      GREEN.println("Using default obsMode config for sequence manager.")
      FileUtils.cpyFileToTmpFromResource("smObsModeConfig.conf")
    }

  private def startSM(obsModeConfigPath: Path, simulation: Boolean): SpawnResponse = {
    smAgent.start()

    val spawnResponse = locationService
      .resolve(AkkaConnection(ComponentId(smAgentPrefix, Machine)), CommonTimeouts.ResolveLocation)
      .flatMap {
        case Some(agentLocation) =>
          new AgentClient(agentLocation)
            .spawnSequenceManager(obsModeConfigPath, isConfigLocal = true, simulation = simulation)
        case None =>
          throw new RuntimeException(
            s"Spawn sequence manager failed: failed to locate agent $smAgentPrefix for spawning sequence manager"
          )
      }
    Await.result(spawnResponse, CommonTimeouts.Wiring)
  }

  private def stopSM(): Unit = {
    val smLocation = Await
      .result(
        locationService
          .resolve(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), CommonTimeouts.ResolveLocation),
        CommonTimeouts.Wiring
      )
      .getOrElse(throw new RuntimeException("Sequence Manager kill failed: Failed to resolve sequence manager"))

    val smAgentLocation = Await
      .result(
        locationService
          .resolve(AkkaConnection(ComponentId(smAgentPrefix, Machine)), CommonTimeouts.ResolveLocation),
        CommonTimeouts.Wiring
      )
      .getOrElse(
        throw new RuntimeException(
          s"Sequence Manager kill failed: Failed to resolve Agent $smAgentPrefix spawning Sequence Manager"
        )
      )

    Await.result(new AgentClient(smAgentLocation).killComponent(smLocation), CommonTimeouts.Wiring)
  }
}
