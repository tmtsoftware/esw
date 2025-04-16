package esw.ocs.app

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import caseapp.RemainingArgs
import caseapp.Command
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.PekkoLocation
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Subsystem
import esw.commons.cli.EswCommand
import esw.commons.utils.location.EswLocationError.RegistrationError
import esw.constants.{CommonTimeouts, SequenceComponentTimeouts}
import esw.http.core.commons.CoordinatedShutdownReasons.FailureReason
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.LoadScript
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{ScriptResponseOrUnhandled, SequencerLocation, Unhandled}
import esw.ocs.app.SequencerAppCommand.*
import esw.ocs.app.simulation.SimulationSequencerWiring
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.internal.SequencerServerFactory

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

/*
 * The main app to start sequencer/sequence component
 */
object SequencerApp extends CommandsEntryPoint {
  // $COVERAGE-OFF$
  def appName: String           = getClass.getSimpleName.dropRight(1) // remove $ from class name
  def appVersion: String        = BuildInfo.version
  override def progName: String = BuildInfo.name
  // $COVERAGE-ON$

  val SequenceComponent: Runner[SeqcompOptions] = Runner[SeqcompOptions]()
  val Sequencer: Runner[SequencerOptions]       = Runner[SequencerOptions]()

  override def commands: Seq[Command[?]] = List(SequenceComponent, Sequencer)

  class Runner[T <: SequencerAppCommand: {Parser, Help}] extends EswCommand[T] {
    override def run(command: T, args: RemainingArgs): Unit = {
      LocationServerStatus.requireUpLocally()
      run(command)
    }

    def run(command: SequencerAppCommand, enableLogging: Boolean = true): SequenceComponentWiring = {
      val wiring = sequenceComponentWiring(command)
      import wiring.actorRuntime.*
      try {
        // irrespective of which command received, Sequence Component needs to be started
        val sequenceCompLocation = reportSequenceComponent(wiring.start())
        if (enableLogging) startLogging(sequenceCompLocation.prefix.toString())
        command match {
          case _: SeqcompOptions => // sequence component is already started
          case SequencerOptions(seqCompSubsystem, _, _, seqSubsystem, obsMode, variation, _) =>
            val subsystem = seqSubsystem.getOrElse(seqCompSubsystem)
            reportSequencer(
              loadAndStartSequencer(subsystem, obsMode, variation, sequenceCompLocation, wiring)
            )
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
      // Note: the expression below is a SAM (single abstract methods) implementation of SequencerServerFactory
      val sequencerServer: SequencerServerFactory =
        if (command.simulation) new SimulationSequencerWiring(_, _).sequencerServer
        else
          new SequencerWiring(_, _).sequencerServer
      new SequenceComponentWiring(command.seqCompSubsystem, command.name, command.agentPrefix, sequencerServer)
    }

    private def loadAndStartSequencer(
        sequencerSubsystem: Subsystem,
        obsMode: ObsMode,
        variation: Option[Variation],
        sequenceComponentLocation: PekkoLocation,
        sequenceComponentWiring: SequenceComponentWiring
    ) = {
      import sequenceComponentWiring.*
      import actorRuntime.*
      val actorRef: ActorRef[SequenceComponentMsg] = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
      val response: Future[ScriptResponseOrUnhandled] =
        (actorRef ? ((replyTo: ActorRef[ScriptResponseOrUnhandled]) =>
          LoadScript(replyTo, sequencerSubsystem, obsMode, variation)
        ))(
          SequenceComponentTimeouts.LoadScript,
          actorRuntime.typedSystem.scheduler
        )

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

    private def reportSequenceComponent(sequencerAppResult: Either[RegistrationError, PekkoLocation]) =
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

}
