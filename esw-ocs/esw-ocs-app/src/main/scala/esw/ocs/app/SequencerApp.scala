package esw.ocs.app

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import caseapp.{CommandApp, RemainingArgs}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.utils.LocationServerStatus
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import esw.http.core.commons.CoordinatedShutdownReasons.FailureReason
import esw.ocs.api.protocol.{LoadScriptError, LoadScriptResponse}
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

  lazy val loggerFactory = new LoggerFactory("sequence component")
  lazy val log: Logger   = loggerFactory.getLogger

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): Unit = {
    val wiring = new SequenceComponentWiring(command.subsystem, command.name, new SequencerWiring(_, _, _).sequencerServer)
    import wiring.actorRuntime._
    try {
      if (enableLogging) startLogging(typedSystem.name)
      // irrespective of which command received, Sequence Component needs to be started
      val sequenceCompLocation = report(wiring.start())
      command match {
        case _: SequenceComponent => // sequence component is already started
        case Sequencer(subsystem, _, id, mode) =>
          report(loadAndStartSequencer(id.getOrElse(subsystem.name), mode, sequenceCompLocation, wiring))
      }
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        shutdown(FailureReason(e)).block
        exit(255)
    }
  }

  private def loadAndStartSequencer(
      id: String,
      mode: String,
      sequenceComponentLocation: AkkaLocation,
      sequenceComponentWiring: SequenceComponentWiring
  ): Either[LoadScriptError, AkkaLocation] = {
    import sequenceComponentWiring._
    import actorRuntime._
    val actorRef: ActorRef[SequenceComponentMsg] = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
    val response: Future[LoadScriptResponse]     = actorRef ? (LoadScript(id, mode, _))
    response.map(_.response).block
  }

  private def report(appResult: Either[LoadScriptError, AkkaLocation]) = appResult match {
    case Left(err) => logAndThrowError(s"Failed to start with error: $err")
    case Right(location) =>
      logInfo(s"Successfully started and registered Component with Location: [$location]")
      location
  }

  private def logAndThrowError(msg: String) = {
    log.error(msg)
    println(s"[ERROR] $msg")
    throw new RuntimeException(msg)
  }

  private def logInfo(msg: String): Unit = {
    log.info(msg)
    println(s"[INFO] $msg")
  }
}
