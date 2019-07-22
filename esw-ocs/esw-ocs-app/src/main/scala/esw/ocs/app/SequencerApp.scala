package esw.ocs.app

import caseapp.{CommandApp, RemainingArgs}
import csw.location.client.utils.LocationServerStatus
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import esw.ocs.api.models.messages.error.RegistrationError
import esw.ocs.app.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.internal.{SequenceComponentWiring, SequencerWiring}

object SequencerApp extends CommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): Unit =
    command match {
      case SequenceComponent(name) =>
        val wiring = new SequenceComponentWiring(name)
        startSequenceComponent(name, wiring, enableLogging)

      case Sequencer(id, mode) =>
        val wiring = new SequencerWiring(id, mode)
        startSequencer(wiring, enableLogging)
    }

  def startSequenceComponent(name: String, sequenceComponentWiring: SequenceComponentWiring, enableLogging: Boolean): Unit = {
    import sequenceComponentWiring.actorRuntime._
    if (enableLogging) startLogging(name)
    report(sequenceComponentWiring.start(), log, enableLogging)(cleanup = typedSystem.terminate())
  }

  def startSequencer(sequencerWiring: SequencerWiring, enableLogging: Boolean): Unit = {
    import sequencerWiring.actorRuntime._
    if (enableLogging) startLogging(sequencerWiring.name)
    report(sequencerWiring.start(), log, enableLogging)(cleanup = typedSystem.terminate())
  }

  private def report(either: Either[RegistrationError, AkkaLocation], log: Logger, enableLogging: Boolean)(
      cleanup: => Unit
  ): Unit =
    either match {
      case Left(err) =>
        log.error(s"Failed to start with error: $err")
        printLogs("ERROR", s"Failed to start application with error: $err", enableLogging)
        cleanup
      case Right(location) =>
        log.info(s"Successfully started and registered Component with Location: [$location]")
        printLogs("INFO", s"Successfully started with Location: $location", enableLogging)
    }

  private def printLogs(level: String, msg: String, enableLogging: Boolean): Unit = if (enableLogging) {
    println(s"[$level] $msg")
    println(s"[$level] Please find complete logs under $$TMT_LOG_HOME directory")
  }
}
