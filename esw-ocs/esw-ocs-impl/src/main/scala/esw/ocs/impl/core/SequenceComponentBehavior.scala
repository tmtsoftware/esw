package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.protocol.ScriptError.{RestartNotAllowedInIdleState, SequenceComponentNotIdle}
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptResponse, RestartScriptResponse, StartSequencerError}
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}

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
        sendReply: Either[StartSequencerError, AkkaLocation] => Unit
    ): Behavior[SequenceComponentMsg] = {
      val sequenceComponentLocation = AkkaLocation(AkkaConnection(ComponentId(prefix, SequenceComponent)), ctx.self.toURI)
      val sequencerServer           = sequencerServerFactory.make(subsystem, observingMode, sequenceComponentLocation)
      val registrationResult        = sequencerServer.start()
      sendReply(registrationResult)
      registrationResult match {
        case Right(location) =>
          log.info(s"Successfully started sequencer for subsystem :$subsystem in observation mode: $observingMode")
          running(subsystem, observingMode, sequencerServer, location)
        case Left(startSequencerScriptError) =>
          log.error(s"Failed to start sequencer: ${startSequencerScriptError.msg}")
          Behaviors.same
      }
    }

    lazy val idle: Behavior[SequenceComponentMsg] = Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
      log.debug(s"Sequence Component in lifecycle state :Idle, received message :[$msg]")
      msg match {
        case LoadScript(subsystem, observingMode, replyTo) =>
          load(ctx, subsystem, observingMode, response => replyTo ! LoadScriptResponse(response))
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(None)
          Behaviors.same
        case UnloadScript(replyTo) =>
          replyTo ! Done
          Behaviors.same
        case Restart(replyTo) =>
          replyTo ! RestartScriptResponse(Left(RestartNotAllowedInIdleState))
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
            unload()
            replyTo ! Done
            idle
          case Restart(replyTo) =>
            unload()
            load(ctx, subsystem, observingMode, sendReply = response => replyTo ! RestartScriptResponse(response))
          case GetStatus(replyTo) =>
            replyTo ! GetStatusResponse(Some(location))
            Behaviors.same
          case LoadScript(_, _, replyTo) =>
            replyTo ! LoadScriptResponse(Left(SequenceComponentNotIdle))
            Behaviors.same
          case Stop => Behaviors.same
        }
      }
    idle
  }
}
