package esw.ocs.app

import caseapp.{CommandApp, RemainingArgs}
import csw.location.client.utils.LocationServerStatus
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.http.core.wiring.ActorRuntime
import esw.ocs.api.protocol.RegistrationError
import esw.ocs.app.SequencerAppCommand._
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}

import scala.util.control.NonFatal

object SequencerApp extends CommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def sequencerWiringWithHttp(
      sequencerId: String,
      observingMode: String,
      sequenceComponentName: Option[String]
  ): SequencerWiring = new SequencerWiring(sequencerId, observingMode, sequenceComponentName)

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): Unit =
    command match {
      case SequenceComponent(subsystem, name) =>
        val wiring                                 = new SequenceComponentWiring(subsystem, name, sequencerWiringWithHttp(_, _, _).sequencerServer)
        val loggerFactory                          = new LoggerFactory("sequence component")
        val genericSequenceComponentLogger: Logger = loggerFactory.getLogger
        startSequenceComponent(wiring, genericSequenceComponentLogger, enableLogging)

      case Sequencer(id, mode) =>
        val wiring: SequencerWiring = sequencerWiringWithHttp(id, mode, None)
        startSequencer(wiring, enableLogging)
    }

  def startSequenceComponent(sequenceComponentWiring: SequenceComponentWiring, logger: Logger, enableLogging: Boolean): Unit = {
    import sequenceComponentWiring._
    withLogging(actorRuntime, logger, enableLogging) {
      sequenceComponentWiring.start()
    }
  }

  def startSequencer(sequencerWiring: SequencerWiring, enableLogging: Boolean): Unit = {
    import sequencerWiring._
    withLogging(actorRuntime, logger, enableLogging) {
      sequencerServer.start()
    }
  }

  private def withLogging(actorRuntime: ActorRuntime, log: Logger, enableLogging: Boolean)(
      f: => Either[RegistrationError, AkkaLocation]
  ): Unit = {
    import actorRuntime._
    def cleanup(): Unit = typedSystem.terminate()
    try {
      if (enableLogging) startLogging(typedSystem.name)
      report(f, log, enableLogging)(() => cleanup())
    } catch {
      case NonFatal(e) => cleanup(); throw e
    }
  }

  private def report(either: Either[RegistrationError, AkkaLocation], log: Logger, enableLogging: Boolean)(
      cleanup: () => Unit
  ): Unit =
    either match {
      case Left(err) =>
        cleanup()
        val errMsg = s"Failed to start with error: $err"
        log.error(errMsg)
        printLogs("ERROR", errMsg, enableLogging)
        exit(255)
      case Right(location) =>
        val msg = s"Successfully started and registered Component with Location: [$location]"
        log.info(msg)
        printLogs("INFO", msg, enableLogging)
    }

  private def printLogs(level: String, msg: String, enableLogging: Boolean): Unit = if (enableLogging) {
    println(s"[$level] $msg")
    println(s"[$level] Please find complete logs under ${sys.env("TMT_LOG_HOME")} directory")
  }
}
