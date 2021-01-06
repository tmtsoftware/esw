package esw.smSimulation.app

import java.nio.file.Path

import akka.actor.CoordinatedShutdown.UnknownReason
import caseapp.RemainingArgs
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import esw.commons.cli.EswCommandApp
import esw.constants.CommonTimeouts
import esw.smSimulation.app.SequenceManagerSimulationCommand._
import esw.smSimulation.app.utils.ResourceReader

import scala.concurrent.Await
import scala.util.control.NonFatal

object SequenceManagerSimulationApp extends EswCommandApp[SequenceManagerSimulationCommand] {

  override def appName: String    = "SequenceManagerSimulationApp" // remove $ from class name
  override def appVersion: String = "0.1.0"
  override def progName: String   = "sm-simulation-app"

  override def run(command: SequenceManagerSimulationCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequenceManagerSimulationCommand, startLogging: Boolean = true) =
    command match {
      case StartCommand(obsModeConfigPath, isConfigLocal, agentPrefix) =>
        start(obsModeConfigPath, isConfigLocal, agentPrefix, startLogging)
    }

  def start(
      obsModeConfigPath: Option[Path],
      isConfigLocal: Boolean,
      agentPrefix: Option[Prefix],
      startLogging: Boolean
  ): SequenceManagerSimulationWiring = {

    lazy val defaultConfPath = ResourceReader.copyToTmp("smSimulationObsMode.conf").getAbsolutePath

    val configPath       = obsModeConfigPath.getOrElse(Path.of(defaultConfPath))
    val simulationWiring = new SequenceManagerSimulationWiring(configPath, isConfigLocal, agentPrefix)
    import simulationWiring._

    try {
      if (startLogging) actorRuntime.startLogging(progName, appVersion)
      logResult(simulationWiring.startSimulation())
      simulationWiring
    }
    catch {
      case NonFatal(e) =>
        Await.result(actorRuntime.shutdown(UnknownReason), CommonTimeouts.Wiring)
        throw e
    }
  }
}
