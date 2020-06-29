package esw.sm.app

import java.nio.file.Path

import caseapp.RemainingArgs
import csw.location.client.utils.LocationServerStatus
import esw.http.core.commons.EswCommandApp
import esw.sm.app.SequenceManagerAppCommand.StartCommand

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
      case StartCommand(configPath) => start(configPath, startLogging)
    }


  // fixme: App ll not terminate on any failure. Use try/catch and shutdown ActorSystem
  def start(configPath: Path, startLogging: Boolean): SequenceManagerWiring = {
    val sequenceManagerWiring = new SequenceManagerWiring(configPath)
    import sequenceManagerWiring._
    if (startLogging) actorRuntime.startLogging(progName, appVersion)
    logResult(sequenceManagerWiring.start())
    sequenceManagerWiring
  }
}
// $COVERAGE-ON$
