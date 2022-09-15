/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.services.apps

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
import esw.constants.{AgentTimeouts, CommonTimeouts}
import esw.services.internal.ManagedService

import java.nio.file.Path
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

// This class is created to start and stop the Sequence Manager on a particular agent
class SequenceManager(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) {

  private val agentConfig: Config = ConfigFactory.load()
  // Prefix of the Agent on which SM will be started
  private val smAgentPrefix: Prefix                = Prefix(ESW, "sm_machine")
  private val smAgent: ManagedService[AgentWiring] = Agent.service(enable = true, smAgentPrefix, agentConfig)
  implicit val executionContext: ExecutionContext  = actorSystem.executionContext

  // Creates an instance of ManagedService with start and stop hook for the SM
  def service(
      enable: Boolean,
      maybeObsModeConfigPath: Option[Path],
      simulation: Boolean
  ): ManagedService[SpawnResponse] =
    ManagedService(
      "sequence-manager",
      enable,
      // start hook for the SM
      () => startSM(getConfig(maybeObsModeConfigPath), simulation),
      // stop hook for the SM
      _ => stopSM()
    )

  // returns the given smObsMode config's path
  // if not given any then returns the default smObsMode config's path present in the resources
  private def getConfig(maybeObsModeConfigPath: Option[Path]): Path =
    maybeObsModeConfigPath.getOrElse {
      GREEN.println("Using default obsMode config for sequence manager.")
      FileUtils.cpyFileToTmpFromResource("smObsModeConfig.conf")
    }

  // this method start an agent and then stats the SM on that agent and return the spawn response
  // it's being called in the start hook for the SM
  private def startSM(obsModeConfigPath: Path, simulation: Boolean): SpawnResponse = {
    smAgent.start()
    val spawnResponseF = locationService
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
    val spawnResponse = Await.result(spawnResponseF, CommonTimeouts.ResolveLocation + AgentTimeouts.SpawnComponent + 2.seconds)
    if (simulation) GREEN.println(s"Sequence manager running in simulation mode.")
    spawnResponse
  }

  // this method stops the started SM (on the smAgent)
  // it's being called in the stop hook for the SM
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
