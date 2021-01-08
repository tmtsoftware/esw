package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.services.utils.ColoredConsole.GREEN
import esw.constants.CommonTimeouts
import esw.services.internal.ManagedService
import esw.sm.app.{SequenceManagerApp, SequenceManagerWiring}

import java.nio.file.{Files, Path}
import scala.concurrent.Await
import scala.io.Source
import scala.util.Using

object SequenceManager {

  def service(
      enable: Boolean,
      maybeObsModeConfigPath: Option[Path],
      agentRunning: Boolean,
      agentPrefix: Option[Prefix]
  ): ManagedService[SequenceManagerWiring] = {
    ManagedService(
      "sequence-manager",
      enable,
      () => startSM(getConfig(maybeObsModeConfigPath), agentPrefixForSM(agentRunning, agentPrefix)),
      stopSM
    )
  }

  private def agentPrefixForSM(agentRunning: Boolean, agentPrefix: Option[Prefix]): Option[Prefix] =
    if (agentRunning) Some(agentPrefix.getOrElse(Prefix(ESW, "primary"))) else None

  private def getConfig(maybeObsModeConfigPath: Option[Path]): Path = {
    maybeObsModeConfigPath.getOrElse({
      val tempConfigPath = Files.createTempFile("sm-", ".conf")
      Using(Source.fromResource("smObsModeConfig.conf")) { reader =>
        Files.write(tempConfigPath, reader.mkString.getBytes)
      }
      tempConfigPath.toFile.deleteOnExit()
      GREEN.println("Using default obsMode config for sequence manager.")
      tempConfigPath
    })
  }

  private def startSM(obsModeConfigPath: Path, agentPrefix: Option[Prefix]): SequenceManagerWiring =
    SequenceManagerApp.start(obsModeConfigPath, isConfigLocal = true, agentPrefix, startLogging = true, simulation = false)

  private def stopSM(smWiring: SequenceManagerWiring): Unit =
    Await.result(smWiring.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
