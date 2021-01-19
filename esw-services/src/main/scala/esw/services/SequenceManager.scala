package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import csw.services.utils.ColoredConsole.GREEN
import esw.agent.akka.app.AgentWiring
import esw.constants.CommonTimeouts
import esw.services.internal.{FileUtils, ManagedService}
import esw.sm.app.{SequenceManagerApp, SequenceManagerWiring}

import java.nio.file.Path
import scala.concurrent.Await

object SequenceManager {

  private val eswAgent: ManagedService[AgentWiring]  = Agent.service(enable = true, Prefix(ESW, "machine1"))
  private val tcsAgent: ManagedService[AgentWiring]  = Agent.service(enable = true, Prefix(TCS, "machine1"))
  private val irisAgent: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(IRIS, "machine1"))
  private val agentsForSimulation                    = List(eswAgent, tcsAgent, irisAgent)

  def service(
      enable: Boolean,
      maybeObsModeConfigPath: Option[Path],
      agentPrefix: Option[Prefix],
      simulation: Boolean
  ): ManagedService[SequenceManagerWiring] =
    ManagedService(
      "sequence-manager",
      enable,
      () => startSM(getConfig(maybeObsModeConfigPath), agentPrefix, simulation),
      w => stopSM(w, simulation)
    )

  private def getConfig(maybeObsModeConfigPath: Option[Path]): Path =
    maybeObsModeConfigPath.getOrElse {
      GREEN.println("Using default obsMode config for sequence manager.")
      FileUtils.cpyFileToTmpFromResource("smObsModeConfig.conf")
    }

  private def startSM(obsModeConfigPath: Path, agentPrefix: Option[Prefix], simulation: Boolean): SequenceManagerWiring = {
    if (simulation) agentsForSimulation.foreach(_.start())
    SequenceManagerApp.start(obsModeConfigPath, isConfigLocal = true, agentPrefix, startLogging = true, simulation)
  }

  private def stopSM(smWiring: SequenceManagerWiring, simulation: Boolean): Unit = {
    if (simulation) agentsForSimulation.foreach(_.stop())
    Await.result(smWiring.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
  }
}
