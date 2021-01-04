package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import esw.constants.CommonTimeouts
import esw.sm.app.{SequenceManagerApp, SequenceManagerWiring}

import java.nio.file.{Files, Path}
import scala.concurrent.Await
import scala.io.Source
import scala.util.Using

object SequenceManager {

  def service(
      enable: Boolean,
      maybeObsModeConfigPath: Option[Path]
  ): ManagedService[SequenceManagerWiring] = {
    ManagedService(
      "sequence-manager",
      enable,
      () => startSM(getConfig(maybeObsModeConfigPath)),
      stopSM
    )
  }

  private def getConfig(maybeObsModeConfigPath: Option[Path]): Path = {
    maybeObsModeConfigPath.getOrElse({
      val tempConfigPath = Files.createTempFile("sm-", ".conf")
      Using(Source.fromResource("smObsModeConfig.conf")) { reader =>
        Files.write(tempConfigPath, reader.mkString.getBytes)
      }
      tempConfigPath.toFile.deleteOnExit()
      tempConfigPath
    })
  }

  private def startSM(obsModeConfigPath: Path): SequenceManagerWiring =
    SequenceManagerApp.start(obsModeConfigPath, isConfigLocal = true, None, startLogging = true)

  private def stopSM(smWiring: SequenceManagerWiring): Unit =
    Await.result(smWiring.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)
}
