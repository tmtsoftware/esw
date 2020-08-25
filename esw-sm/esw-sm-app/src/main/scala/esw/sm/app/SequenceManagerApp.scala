package esw.sm.app

import java.nio.file.Path

import akka.actor.CoordinatedShutdown.UnknownReason
import caseapp.RemainingArgs
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import esw.commons.Timeouts
import esw.commons.cli.EswCommandApp
import esw.sm.app.SequenceManagerAppCommand._

import scala.concurrent.Await
import scala.util.control.NonFatal

// $COVERAGE-OFF$
object SequenceManagerApp extends EswCommandApp[SequenceManagerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: SequenceManagerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequenceManagerAppCommand, startLogging: Boolean = true): SequenceManagerWiring =
    command match {
      case StartCommand(obsModeConfigPath, isConfigLocal, agentPrefix) =>
        start(obsModeConfigPath, isConfigLocal, agentPrefix, startLogging)
    }

  // fixme: App ll not terminate on any failure. Use try/catch and shutdown ActorSystem
  def start(
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      agentPrefix: Option[Prefix],
      startLogging: Boolean
  ): SequenceManagerWiring = {
    val sequenceManagerWiring = new SequenceManagerWiring(obsModeConfigPath, isConfigLocal, agentPrefix)
    import sequenceManagerWiring._

    try {
      if (startLogging) actorRuntime.startLogging(progName, appVersion)
      logResult(sequenceManagerWiring.start())
      sequenceManagerWiring
    }
    catch {
      case NonFatal(e) =>
        Await.result(actorRuntime.shutdown(UnknownReason), Timeouts.DefaultTimeout)
        throw e
    }
  }
}
// $COVERAGE-ON$
