package esw.ocs.impl.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.actor.messages.{
  IdleStateSequenceComponentMsg,
  RunningStateSequenceComponentMsg,
  SequenceComponentMsg,
  ShuttingDownStateSequenceComponentMsg,
  UnhandleableSequenceComponentMsg
}
import esw.ocs.api.models.SequenceComponentState
import esw.ocs.api.protocol.SequenceComponentResponse._
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

class SequenceComponentBehavior(
    prefix: Prefix,
    log: Logger,
    locationService: LocationService,
    sequencerServerFactory: SequencerServerFactory
)(implicit actorSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  def idle: Behavior[SequenceComponentMsg] =
    receive[IdleStateSequenceComponentMsg](SequenceComponentState.Idle) { (ctx, msg) =>
      log.debug(s"Sequence Component in lifecycle state :Idle, received message :[$msg]")
      msg match {
        case LoadScript(subsystem, observingMode, replyTo) =>
          load(ctx, subsystem, observingMode, replyTo)
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(None)
          Behaviors.same
        case UnloadScript(replyTo) =>
          replyTo ! Ok
          Behaviors.same
        case Stop              => Behaviors.stopped
        case Shutdown(replyTo) => shutdown(ctx.self, replyTo, None)
      }
    }

  private def load(
      ctx: ActorContext[SequenceComponentMsg],
      subsystem: Subsystem,
      observingMode: String,
      replyTo: ActorRef[ScriptResponseOrUnhandled]
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
    receive[RunningStateSequenceComponentMsg](SequenceComponentState.Running) { (ctx, msg) =>
      log.debug(s"Sequence Component in lifecycle state :Running, received message :[$msg]")

      def unload(): Unit = {
        sequencerServer.shutDown()
        log.info("Unloaded script successfully")
      }

      msg match {
        case UnloadScript(replyTo) =>
          unload()
          replyTo ! Ok
          idle
        case Restart(replyTo) =>
          unload()
          load(ctx, subsystem, observingMode, replyTo)
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(Some(location))
          Behaviors.same
        case Stop              => Behaviors.same
        case Shutdown(replyTo) => shutdown(ctx.self, replyTo, Some(sequencerServer))
      }
    }

  private def shutdown(
      self: ActorRef[SequenceComponentMsg],
      replyTo: ActorRef[OkOrUnhandled],
      sequencerServer: Option[SequencerServer]
  ): Behavior[SequenceComponentMsg] = {
    sequencerServer.foreach(_.shutDown())
    locationService.unregister(AkkaConnection(ComponentId(prefix, SequenceComponent))).map { _ =>
      self ! ShutdownInternal(replyTo)
    }
    shuttingDown()
  }

  private def shuttingDown(): Behavior[SequenceComponentMsg] =
    receive[ShuttingDownStateSequenceComponentMsg](SequenceComponentState.ShuttingDown) { (_, msg) =>
      msg match {
        case ShutdownInternal(replyTo) =>
          replyTo ! Ok
          actorSystem.terminate()
          Behaviors.stopped
      }
    }

  private def receive[HandleableMsg <: SequenceComponentMsg: ClassTag](
      state: SequenceComponentState
  )(
      handler: (ActorContext[SequenceComponentMsg], HandleableMsg) => Behavior[SequenceComponentMsg]
  ): Behavior[SequenceComponentMsg] =
    Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
      msg match {
        case msg: HandleableMsg => handler(ctx, msg)
        case msg: UnhandleableSequenceComponentMsg =>
          msg.replyTo ! Unhandled(state, msg.getClass.getSimpleName)
          Behaviors.same
      }
    }
}
