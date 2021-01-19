package esw.sm.app

import akka.actor.CoordinatedShutdown.UnknownReason
import caseapp.RemainingArgs
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import esw.commons.cli.EswCommandApp
import esw.commons.utils.files.FileUtils
import esw.constants.CommonTimeouts
import esw.sm.app.SequenceManagerAppCommand._

import java.nio.file.Path
import scala.concurrent.Await
import scala.util.control.NonFatal

// $COVERAGE-OFF$
object SequenceManagerApp extends EswCommandApp[SequenceManagerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version

  override def progName: String = BuildInfo.name

  override def run(command: SequenceManagerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequenceManagerAppCommand, startLogging: Boolean = true): SequenceManagerWiring =
    command match {
      case StartCommand(obsModeConfigPath, isConfigLocal, agentPrefix, simulation) => {
        if (simulation) {
          lazy val defaultConfPath = FileUtils.cpyFileToTmpFromResource("smSimulationObsMode.conf")
          lazy val configPath      = obsModeConfigPath.getOrElse(defaultConfPath)
          start(configPath, isConfigLocal = true, agentPrefix, startLogging, simulation)
        }
        else
          start(obsModeConfigPath.get, isConfigLocal, agentPrefix, startLogging, simulation)
      }
    }

  def start(
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      agentPrefix: Option[Prefix],
      startLogging: Boolean,
      simulation: Boolean
  ): SequenceManagerWiring = {

    val sequenceManagerWiring = new SequenceManagerWiring(obsModeConfigPath, isConfigLocal, agentPrefix, simulation)
    import sequenceManagerWiring._

    try {
      if (startLogging) actorRuntime.startLogging(progName, appVersion)
      logResult(sequenceManagerWiring.start())
      sequenceManagerWiring
    }
    catch {
      case NonFatal(e) =>
        Await.result(actorRuntime.shutdown(UnknownReason), CommonTimeouts.Wiring)
        throw e
    }
  }
}
// $COVERAGE-ON$
