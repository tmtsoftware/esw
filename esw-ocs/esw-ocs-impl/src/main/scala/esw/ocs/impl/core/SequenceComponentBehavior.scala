package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.{AkkaLocation, ComponentId}
import csw.location.models.Connection.AkkaConnection
import csw.logging.api.scaladsl.Logger
import csw.params.core.models.{Prefix, Subsystem}
import esw.ocs.api.protocol.{GetStatusResponse, ScriptError, ScriptResponse}
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg._
import csw.location.api.extensions.ActorExtension._

object SequenceComponentBehavior {

  def behavior(
      prefix: Prefix,
      log: Logger,
      sequencerServerFactory: SequencerServerFactory
  ): Behavior[SequenceComponentMsg] = {

    def load(
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
          log.info(s"Successfully started sequencer with sequencer id :$subsystem in observation mode: $observingMode")
          running(subsystem, observingMode, sequencerServer, location)
        case Left(scriptError) =>
          log.error(s"Failed to start sequencer: ${scriptError.msg}")
          Behaviors.same
      }
    }

    lazy val idle: Behavior[SequenceComponentMsg] = Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
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
          replyTo ! ScriptResponse(Left(ScriptError("Restart is not supported in idle state")))
          Behaviors.same

        case Stop => Behaviors.stopped
      }
    }

    def running(subsystem: Subsystem, observingMode: String, sequencerServer: SequencerServer, location: AkkaLocation) =
      Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
        log.debug(s"Sequence Component in lifecycle state :Running, received message :[$msg]")

        def unload(): Unit = {
          sequencerServer.shutDown()
          log.info("Unloaded script successfully")
        }

        msg match {
          case UnloadScript(replyTo) =>
            unload(); replyTo ! Done
            idle
          case Restart(replyTo) =>
            unload()
            load(ctx, subsystem, observingMode, replyTo)
          case GetStatus(replyTo) =>
            replyTo ! GetStatusResponse(Some(location))
            Behaviors.same
          case LoadScript(_, _, replyTo) =>
            replyTo ! ScriptResponse(Left(ScriptError("Loading script failed: Sequencer already running")))
            Behaviors.same
          case Stop => Behaviors.same
        }
      }
    idle
  }
}
