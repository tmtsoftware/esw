package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import csw.prefix.models.Prefix
import csw.services.utils.ColoredConsole.GREEN
import esw.constants.CommonTimeouts
import esw.services.internal.{FileUtils, ManagedService}
import esw.sm.app.{SequenceManagerApp, SequenceManagerWiring}

import java.nio.file.Path
import scala.concurrent.Await

object SequenceManager {

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
      stopSM
    )

  private def getConfig(maybeObsModeConfigPath: Option[Path]): Path =
    maybeObsModeConfigPath.getOrElse {
      GREEN.println("Using default obsMode config for sequence manager.")
      FileUtils.cpyFileToTmpFromResource("smObsModeConfig.conf")
    }

  private def startSM(obsModeConfigPath: Path, agentPrefix: Option[Prefix], simulation: Boolean): SequenceManagerWiring =
    SequenceManagerApp.start(obsModeConfigPath, isConfigLocal = true, agentPrefix, startLogging = true, simulation)

  private def stopSM(smWiring: SequenceManagerWiring): Unit =
    Await.result(smWiring.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
