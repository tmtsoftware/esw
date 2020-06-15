package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.protocol.ScriptError.{RestartNotSupportedInIdle, SequenceComponentNotIdle}
import esw.ocs.api.protocol.{GetStatusResponse, ScriptResponse}
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}

import scala.concurrent.ExecutionContext

class SequenceComponentBehavior(
    prefix: Prefix,
    log: Logger,
    locationService: LocationService,
    sequencerServerFactory: SequencerServerFactory
)(implicit actorSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  def idle: Behavior[SequenceComponentMsg] =
    Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
      log.debug(s"Sequence Component in lifecycle state :Idle, received message :[$msg]")
      msg match {
        case LoadScript(subsystem, observingMode, replyTo) =>
          load(ctx, subsystem, observingMode, replyTo)
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(None)
          Behaviors.same
        case UnloadScript(replyTo) =>
          replyTo ! Done
          Behaviors.same
        case Restart(replyTo) =>
          replyTo ! ScriptResponse(Left(RestartNotSupportedInIdle))
          Behaviors.same

        case Stop                => Behaviors.stopped
        case Shutdown(replyTo)   => shutdown(ctx.self, replyTo, None)
        case ShutdownInternal(_) => Behaviors.unhandled
      }
    }

  private def load(
      ctx: ActorContext[SequenceComponentMsg],
      subsystem: Subsystem,
      observingMode: String,
      replyTo: ActorRef[ScriptResponse]
  ): Behavior[SequenceComponentMsg] = {
    val sequenceComponentLocation = AkkaLocation(AkkaConnection(ComponentId(prefix, SequenceComponent)), ctx.self.toURI)
    val sequencerServer           = sequencerServerFactory.make(subsystem, observingMode, sequenceComponentLocation)
    val registrationResult        = sequencerServer.start()
    replyTo ! ScriptResponse(registrationResult)
    registrationResult match {
      case Right(location) =>
        log.info(s"Successfully started sequencer for subsystem :$subsystem in observation mode: $observingMode")
        running(subsystem, observingMode, sequencerServer, location)
      case Left(scriptError) =>
        log.error(s"Failed to start sequencer: ${scriptError.msg}")
        Behaviors.same
    }
  }

  private def running(subsystem: Subsystem, observingMode: String, sequencerServer: SequencerServer, location: AkkaLocation) =
    Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
      log.debug(s"Sequence Component in lifecycle state :Running, received message :[$msg]")

      def unload(): Unit = {
        sequencerServer.shutDown()
        log.info("Unloaded script successfully")
      }

      msg match {
        case UnloadScript(replyTo) =>
          unload()
          replyTo ! Done
          idle
        case Restart(replyTo) =>
          unload()
          load(ctx, subsystem, observingMode, replyTo)
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(Some(location))
          Behaviors.same
        case LoadScript(_, _, replyTo) =>
          replyTo ! ScriptResponse(Left(SequenceComponentNotIdle(Prefix(subsystem, observingMode))))
          Behaviors.same
        case Stop                => Behaviors.same
        case Shutdown(replyTo)   => shutdown(ctx.self, replyTo, Some(sequencerServer))
        case ShutdownInternal(_) => Behaviors.unhandled
      }
    }

  private def shutdown(
      self: ActorRef[SequenceComponentMsg],
      replyTo: ActorRef[Done],
      sequencerServer: Option[SequencerServer]
  ): Behavior[SequenceComponentMsg] = {
    sequencerServer.foreach(_.shutDown())
    locationService.unregister(AkkaConnection(ComponentId(prefix, SequenceComponent))).map { _ =>
      self ! ShutdownInternal(replyTo)
    }
    shuttingDown()
  }

  private def shuttingDown(): Behavior[SequenceComponentMsg] =
    Behaviors.receiveMessagePartial {
      case ShutdownInternal(replyTo) =>
        replyTo ! Done
        actorSystem.terminate()
        Behaviors.stopped
    }
}
