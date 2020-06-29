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
import esw.commons.extensions.EitherExt._
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.actor.messages._
import esw.ocs.api.models.{ObsMode, SequenceComponentState}
import esw.ocs.api.protocol.ScriptError
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
  private val akkaConnection: AkkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
  implicit val ec: ExecutionContext          = actorSystem.executionContext

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
        case Shutdown(replyTo) => shutdown(replyTo, None)
      }
    }

  private def load(
      ctx: ActorContext[SequenceComponentMsg],
      subsystem: Subsystem,
      observingMode: ObsMode,
      replyTo: ActorRef[ScriptResponseOrUnhandled]
  ): Behavior[SequenceComponentMsg] = {
    val sequenceComponentLocation = AkkaLocation(akkaConnection, ctx.self.toURI)
    val sequencerServer           = sequencerServerFactory.make(subsystem, observingMode, sequenceComponentLocation)
    val registrationResult        = sequencerServer.start().mapToAdt(location => SequencerLocation(location), identity)
    replyTo ! registrationResult

    registrationResult match {
      case SequencerLocation(location) =>
        log.info(s"Successfully started sequencer for subsystem :$subsystem in observation mode: ${observingMode.name}")
        running(subsystem, observingMode, sequencerServer, location)
      case error: ScriptError =>
        log.error(s"Failed to start sequencer: ${error.msg}")
        Behaviors.same
    }
  }

  private def running(subsystem: Subsystem, observingMode: ObsMode, sequencerServer: SequencerServer, location: AkkaLocation) =
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
        case RestartScript(replyTo) =>
          unload()
          load(ctx, subsystem, observingMode, replyTo)
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(Some(location))
          Behaviors.same
        case Shutdown(replyTo) => shutdown(replyTo, Some(sequencerServer))
        case Stop              => Behaviors.same
      }
    }

  private def shutdown(
      replyTo: ActorRef[Ok.type],
      sequencerServer: Option[SequencerServer]
  ): Behavior[SequenceComponentMsg] = {
    sequencerServer.foreach(_.shutDown())

    locationService
      .unregister(akkaConnection)
      .onComplete(_ => {
        replyTo ! Ok
        actorSystem.terminate()
      })
    Behaviors.stopped
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
