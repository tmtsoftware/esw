package esw.ocs.app

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import caseapp.RemainingArgs
import com.typesafe.config.ConfigFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Subsystem
import esw.commons.cli.EswCommandApp
import esw.commons.utils.location.EswLocationError.RegistrationError
import esw.constants.{CommonTimeouts, SequenceComponentTimeouts}
import esw.http.core.commons.CoordinatedShutdownReasons.FailureReason
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.LoadScript
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{ScriptResponseOrUnhandled, SequencerLocation, Unhandled}
import esw.ocs.app.SequencerAppCommand._
import esw.ocs.app.simulation.SimulationSequencerWiring
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.internal.SequencerServerFactory

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object SequencerApp extends EswCommandApp[SequencerAppCommand] {
  // $COVERAGE-OFF$
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name
  // $COVERAGE-ON$

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    val config = ConfigFactory.load()
    println("####################")
    println(config.getConfig("csw-logging"))
    run(command)
  }

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): SequenceComponentWiring = {
    val wiring = sequenceComponentWiring(command)
    import wiring.actorRuntime._
    try {
      // irrespective of which command received, Sequence Component needs to be started
      val sequenceCompLocation = reportSequenceComponent(wiring.start())
      if (enableLogging) {
        startLogging(sequenceCompLocation.prefix.toString())
        // TODO to remove
        log.info("Logging System started")
        println("Logging System started")
      }
      command match {
        case _: SequenceComponent => // sequence component is already started
        case Sequencer(seqCompSubsystem, _, _, seqSubsystem, mode, _) =>
          reportSequencer(loadAndStartSequencer(seqSubsystem.getOrElse(seqCompSubsystem), mode, sequenceCompLocation, wiring))
      }
    }
    catch {
      case NonFatal(e) =>
        Await.result(shutdown(FailureReason(e)), CommonTimeouts.Wiring)
        throw e
    }
    wiring
  }

  def sequenceComponentWiring(command: SequencerAppCommand): SequenceComponentWiring = {
    val sequencerServer: SequencerServerFactory =
      if (command.simulation) new SimulationSequencerWiring(_, _, _).sequencerServer
      else new SequencerWiring(_, _, _).sequencerServer
    new SequenceComponentWiring(command.seqCompSubsystem, command.name, command.agentPrefix, sequencerServer)
  }

  private def loadAndStartSequencer(
      subsystem: Subsystem,
      mode: ObsMode,
      sequenceComponentLocation: AkkaLocation,
      sequenceComponentWiring: SequenceComponentWiring
  ): ScriptResponseOrUnhandled = {
    import sequenceComponentWiring._
    import actorRuntime._
    val actorRef: ActorRef[SequenceComponentMsg] = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
    val response: Future[ScriptResponseOrUnhandled] =
      (actorRef ? (LoadScript(subsystem, mode, _)))(SequenceComponentTimeouts.LoadScript, actorRuntime.typedSystem.scheduler)

    Await.result(response, CommonTimeouts.Wiring)
  }

  private def reportSequencer(seqCompAppResult: ScriptResponseOrUnhandled) = {
    seqCompAppResult match {
      case Unhandled(_, _, msg) =>
        logAndThrowError(log, msg, new RuntimeException(s"Failed to start with error: $msg"))
      case SequencerLocation(location) =>
        logInfo(
          log,
          s"Successfully started and registered ${location.connection.componentId.componentType} with Location: [$location]"
        )
        location
      case error: ScriptError =>
        logAndThrowError(log, error.msg, new RuntimeException(s"Failed to start with error: ${error.msg}"))
    }
  }

  private def reportSequenceComponent(sequencerAppResult: Either[RegistrationError, AkkaLocation]) =
    sequencerAppResult match {
      case Left(err) =>
        val msg = s"Failed to start with error: ${err.msg}"
        logAndThrowError(log, msg, new RuntimeException(msg))
      case Right(location) =>
        logInfo(
          log,
          s"Successfully started and registered ${location.connection.componentId.componentType} with Location: [$location]"
        )
        location
    }
}
