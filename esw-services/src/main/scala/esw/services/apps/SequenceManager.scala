package esw.services.apps

import java.nio.file.Path

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import csw.services.utils.ColoredConsole.GREEN
import esw.agent.akka.app.AgentWiring
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.SpawnResponse
import esw.commons.utils.files.FileUtils
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService

import scala.concurrent.{Await, ExecutionContext}

class SequenceManager(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) {

  private val agentConfig: Config                    = ConfigFactory.load()
  private val eswAgent1Prefix: Prefix                = Prefix(ESW, "machine1")
  private val eswAgent1: ManagedService[AgentWiring] = Agent.service(enable = true, eswAgent1Prefix, agentConfig)
  private val eswAgent2: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(ESW, "machine2"), agentConfig)
  private val tcsAgent: ManagedService[AgentWiring]  = Agent.service(enable = true, Prefix(TCS, "machine1"), agentConfig)
  private val irisAgent: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(IRIS, "machine1"), agentConfig)
  private val agentsForSimulation                    = List(eswAgent2, tcsAgent, irisAgent)
  implicit val executionContext: ExecutionContext    = actorSystem.executionContext

  def service(
      enable: Boolean,
      maybeObsModeConfigPath: Option[Path],
      simulation: Boolean
  ): ManagedService[SpawnResponse] =
    ManagedService(
      "sequence-manager",
      enable,
      () => startSM(getConfig(maybeObsModeConfigPath), simulation),
      r => stopSM(r, simulation)
    )

  private def getConfig(maybeObsModeConfigPath: Option[Path]): Path =
    maybeObsModeConfigPath.getOrElse {
      GREEN.println("Using default obsMode config for sequence manager.")
      FileUtils.cpyFileToTmpFromResource("smObsModeConfig.conf")
    }

  private def startSM(obsModeConfigPath: Path, simulation: Boolean): SpawnResponse = {
    if (simulation) agentsForSimulation.foreach(_.start())

    eswAgent1.start()

    val spawnResponse = locationService
      .resolve(AkkaConnection(ComponentId(eswAgent1Prefix, Machine)), CommonTimeouts.ResolveLocation)
      .flatMap {
        case Some(agentLocation) =>
          new AgentClient(agentLocation)
            .spawnSequenceManager(obsModeConfigPath, isConfigLocal = true, simulation = simulation)
        case None =>
          throw new RuntimeException(
            s"Spawn sequence manager failed: failed to locate agent $eswAgent1Prefix for spawning sequence manager"
          )
      }
    Await.result(spawnResponse, CommonTimeouts.Wiring)
  }

  private def stopSM(spawnResponse: SpawnResponse, simulation: Boolean): Unit = {
    if (simulation) agentsForSimulation.foreach(_.stop())

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
          .resolve(AkkaConnection(ComponentId(eswAgent1Prefix, Machine)), CommonTimeouts.ResolveLocation),
        CommonTimeouts.Wiring
      )
      .getOrElse(
        throw new RuntimeException(
          s"Sequence Manager kill failed: Failed to resolve Agent $eswAgent1Prefix spawning Sequence Manager"
        )
      )

    Await.result(new AgentClient(smAgentLocation).killComponent(smLocation), CommonTimeouts.Wiring)
  }
}
