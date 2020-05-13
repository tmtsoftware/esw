package esw.ocs.app

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import caseapp.RemainingArgs
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.location.client.utils.LocationServerStatus
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import csw.prefix.models.Subsystem
import esw.commons.Timeouts
import esw.http.core.commons.CoordinatedShutdownReasons.FailureReason
import esw.http.core.commons.EswCommandApp
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.LoadScript
import esw.ocs.api.protocol.{LoadScriptError, LoadScriptResponse}
import esw.ocs.app.SequencerAppCommand._
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object SequencerApp extends EswCommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  lazy val log: Logger = GenericLoggerFactory.getLogger

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): SequenceComponentWiring = {
    val wiring = new SequenceComponentWiring(command.seqCompSubsystem, command.name, new SequencerWiring(_, _, _).sequencerServer)
    import wiring.actorRuntime._
    try {
      // irrespective of which command received, Sequence Component needs to be started
      val sequenceCompLocation = report(wiring.start())
      if (enableLogging) startLogging(sequenceCompLocation.prefix.toString())
      command match {
        case _: SequenceComponent => // sequence component is already started
        case Sequencer(seqCompSubsystem, _, seqSubsystem, mode) =>
          report(loadAndStartSequencer(seqSubsystem.getOrElse(seqCompSubsystem), mode, sequenceCompLocation, wiring))
      }
    }
    catch {
      case NonFatal(e) =>
        Await.result(shutdown(FailureReason(e)), Timeouts.DefaultTimeout)
        throw e
    }
    wiring
  }

  private def loadAndStartSequencer(
      subsystem: Subsystem,
      mode: String,
      sequenceComponentLocation: AkkaLocation,
      sequenceComponentWiring: SequenceComponentWiring
  ): Either[LoadScriptError, AkkaLocation] = {
    import sequenceComponentWiring._
    import actorRuntime._
    val actorRef: ActorRef[SequenceComponentMsg] = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
    val response: Future[LoadScriptResponse]     = actorRef ? (LoadScript(subsystem, mode, _))

    Await.result(response.map(_.response), Timeouts.DefaultTimeout)
  }

  private def report(appResult: Either[LoadScriptError, AkkaLocation]) =
    appResult match {
      case Left(err) => logAndThrowError(log, s"Failed to start with error: $err")
      case Right(location) =>
        logInfo(
          log,
          s"Successfully started and registered ${location.connection.componentId.componentType} with Location: [$location]"
        )
        location
    }

}
