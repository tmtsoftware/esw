package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import esw.sm.app.{SequenceManagerApp, SequenceManagerWiring}

import java.nio.file.Path

object SequenceManager {

  def service(
      enable: Boolean,
      obsModeConfigPath: Option[Path]
  ): ManagedService[SequenceManagerWiring] = {
    ManagedService(
      "sequence-manager",
      enable,
      () => startSM(obsModeConfigPath),
      stopSM
    )
  }

  private def startSM(obsModeConfigPath: Option[Path]): Option[SequenceManagerWiring] =
    obsModeConfigPath.map(p => {
      SequenceManagerApp.start(p, isConfigLocal = true, None, startLogging = true)
    })

  private def stopSM(smWiring: SequenceManagerWiring): Unit = {
    smWiring.shutdown(ActorSystemTerminateReason)
  }
}
