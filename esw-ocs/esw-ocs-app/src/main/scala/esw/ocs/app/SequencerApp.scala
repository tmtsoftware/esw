package esw.ocs.app

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import caseapp.{CommandApp, RemainingArgs}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.utils.LocationServerStatus
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.core.models.Subsystem
import esw.http.core.wiring.ActorRuntime
import esw.ocs.api.protocol.{LoadScriptResponse, RegistrationError}
import esw.ocs.app.SequencerAppCommand._
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.LoadScript
import esw.ocs.impl.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future
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
      packageId: String,
      observingMode: String,
      sequenceComponentName: Option[String]
  ): SequencerWiring = new SequencerWiring(packageId, observingMode, sequenceComponentName)

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): Unit =
    command match {
      case SequenceComponent(subsystem, name) =>
        startSequenceComponent(subsystem, name, enableLogging)

      case Sequencer(subsystem, name, id, mode) =>
        val sequenceComponentWiring =
          new SequenceComponentWiring(subsystem, name, sequencerWiringWithHttp(_, _, _).sequencerServer)
        val loggerFactory  = new LoggerFactory("sequence component")
        val logger: Logger = loggerFactory.getLogger
        import sequenceComponentWiring._
        withLogging(actorRuntime, logger, enableLogging) {
          loadAndStartSequencer(id.getOrElse(subsystem.name), mode, sequenceComponentWiring)
        }
    }

  def startSequenceComponent(subsystem: Subsystem, name: Option[String], enableLogging: Boolean): Unit = {
    val wiring         = new SequenceComponentWiring(subsystem, name, sequencerWiringWithHttp(_, _, _).sequencerServer)
    val loggerFactory  = new LoggerFactory("sequence component")
    val logger: Logger = loggerFactory.getLogger
    import wiring._
    withLogging(actorRuntime, logger, enableLogging) {
      wiring.start()
    }
  }

  private def loadAndStartSequencer(
      id: String,
      mode: String,
      sequenceComponentWiring: SequenceComponentWiring
  ): Either[RegistrationError, AkkaLocation] = {
    import sequenceComponentWiring._
    import sequenceComponentWiring.actorRuntime.{scheduler, _}
    import sequenceComponentWiring.cswWiring.actorSystem

    val wiring = sequenceComponentWiring.start()
    wiring.flatMap { akkaLocation =>
      val actorRef: ActorRef[SequenceComponentMsg] =
        akkaLocation.uri.toActorRef(actorSystem).unsafeUpcast[SequenceComponentMsg]

      val response: Future[LoadScriptResponse] = actorRef ? (LoadScript(id, mode, _))
      response.map(_.response).block
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
      case NonFatal(e) =>
        cleanup(); throw e
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
