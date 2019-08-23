package esw.ocs.app

import caseapp.{CommandApp, RemainingArgs}
import csw.framework.internal.wiring.ActorRuntime
import csw.location.client.utils.LocationServerStatus
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import esw.ocs.api.models.messages.RegistrationError
import esw.ocs.app.SequencerAppCommand._
import esw.ocs.internal.{SequenceComponentWiring, SequencerWiring}

import scala.util.control.NonFatal

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
      case SequenceComponent(prefix) =>
        val wiring = new SequenceComponentWiring(prefix)
        startSequenceComponent(wiring, enableLogging)

      case Sequencer(id, mode) =>
        val wiring = new SequencerWiring(id, mode, None)
        startSequencer(wiring, enableLogging)
    }

  def startSequenceComponent(sequenceComponentWiring: SequenceComponentWiring, enableLogging: Boolean): Unit = {
    import sequenceComponentWiring._
    withLogging(actorRuntime, cswServicesWiring.log, enableLogging) {
      sequenceComponentWiring.start()
    }
  }

  def startSequencer(sequencerWiring: SequencerWiring, enableLogging: Boolean): Unit = {
    import sequencerWiring._
    withLogging(actorRuntime, cswServicesWiring.log, enableLogging) {
      sequencerWiring.start()
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
