package esw.ocs.impl.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.ocs.api.actor.messages.*
import esw.ocs.api.actor.messages.SequenceComponentMsg.*
import esw.ocs.api.models.{SequenceComponentState, Variation}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.*
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

/**
 * Behavior class for Sequence component's actor. Currently the sequence component behavior is base of the state(Idle and Running).
 * Idle represents the state in which there are no Sequencer running in the Sequence Component
 * On the other hand, Running state means that there is Sequencer running in the Sequence Component
 *
 * @param prefix - prefix of the sequence component
 * @param log - logger
 * @param locationService - an instance of Location Service
 * @param sequencerServerFactory - an Instance of SequencerServerFactory
 * @param actorSystem - an Akka ActorSystem
 */
class SequenceComponentBehavior(
    prefix: Prefix,
    log: Logger,
    locationService: LocationService,
    sequencerServerFactory: SequencerServerFactory
)(implicit actorSystem: ActorSystem[SpawnProtocol.Command]) {
  private val akkaConnection: AkkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
  implicit val ec: ExecutionContext          = actorSystem.executionContext

  /*
   * Sequence Component Behaviour when it is in Idle state means there are no Sequencer running
   * @return an typed actor [[akka.actor.typed.Behavior]] which only supports [[esw.ocs.api.actor.messages.SequenceComponentMsg]]
   */
  def idle: Behavior[SequenceComponentMsg] =
    receive[IdleStateSequenceComponentMsg](SequenceComponentState.Idle) { (_, msg) =>
      log.debug(s"Sequence Component in lifecycle state :Idle, received message :[$msg]")
      msg match {
        case LoadScript(replyTo, subsystem, obsMode, variation) =>
          val sequencerPrefix = Variation.prefix(subsystem, obsMode, variation)
          load(sequencerPrefix, replyTo)
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

  /*
   * This method is to start the sequencer with the given subsystem and obsMode
   * If successful it will transition the sequence component's behavior into Running state
   * If failed it will remain in Idle state
   */
  private def load(sequencerPrefix: Prefix, replyTo: ActorRef[ScriptResponseOrUnhandled]): Behavior[SequenceComponentMsg] = {
    val sequencerServer    = sequencerServerFactory.make(sequencerPrefix, prefix)
    val registrationResult = sequencerServer.start().fold(identity, SequencerLocation.apply)
    replyTo ! registrationResult

    registrationResult match {
      case SequencerLocation(location) =>
        log.info(
          s"Successfully started sequencer for subsystem :${prefix.subsystem} in observation mode: ${prefix.componentName}"
        )
        running(sequencerPrefix, sequencerServer, location)
      case error: ScriptError =>
        log.error(s"Failed to start sequencer: ${error.msg}")
        Behaviors.same
    }
  }

  /*
   * Sequence Component Behaviour when it is in Running state means there is a Sequencer running
   * @return an typed actor [[akka.actor.typed.Behavior]] which only supports [[esw.ocs.api.actor.messages.SequenceComponentMsg]]
   */
  private def running(sequencerPrefix: Prefix, sequencerServer: SequencerServer, location: AkkaLocation) =
    receive[RunningStateSequenceComponentMsg](SequenceComponentState.Running) { (_, msg) =>
      log.debug(s"Sequence Component in lifecycle state :Running, received message :[$msg]")

      // unloads script (stops the sequencer)
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
          load(sequencerPrefix, replyTo)
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(Some(location))
          Behaviors.same
        case Shutdown(replyTo) => shutdown(replyTo, Some(sequencerServer))
        case Stop              => Behaviors.same
      }
    }

  // Method to shutdown the sequence component
  private def shutdown(
      replyTo: ActorRef[Ok.type],
      sequencerServer: Option[SequencerServer]
  ): Behavior[SequenceComponentMsg] = {
    sequencerServer.foreach(_.shutDown())

    locationService
      .unregister(akkaConnection)
      .onComplete { _ =>
        replyTo ! Ok
        actorSystem.terminate()
      }
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
        case x => throw new MatchError(x)
      }
    }
}
