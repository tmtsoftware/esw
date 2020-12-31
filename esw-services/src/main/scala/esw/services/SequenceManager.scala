package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import esw.constants.CommonTimeouts
import esw.services.utils.PathUtils
import esw.sm.app.{SequenceManagerApp, SequenceManagerWiring}

import java.nio.file.Path
import scala.concurrent.Await

object SequenceManager {

  def service(
      enable: Boolean,
      maybeObsModeConfigPath: Option[Path]
  ): ManagedService[SequenceManagerWiring] = {
    val obsModeConfigPath = maybeObsModeConfigPath.getOrElse(PathUtils.getResourcePath("smObsModeConfig.conf"))
    ManagedService(
      "sequence-manager",
      enable,
      () => startSM(obsModeConfigPath),
      stopSM
    )
  }

  private def startSM(obsModeConfigPath: Path): SequenceManagerWiring =
    SequenceManagerApp.start(obsModeConfigPath, isConfigLocal = true, None, startLogging = true)

  private def stopSM(smWiring: SequenceManagerWiring): Unit = {
    Await.result(smWiring.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
  }
}
