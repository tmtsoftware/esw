package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptResponse, LoadScriptError}
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.{GetStatus, LoadScript, Stop, UnloadScript}

object SequenceComponentBehavior {

  def behavior(
      sequenceComponentName: String,
      log: Logger,
      sequencerServerFactory: SequencerServerFactory
  ): Behavior[SequenceComponentMsg] = {

    lazy val idle: Behavior[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] { msg =>
      log.debug(s"Sequence Component in lifecycle state :Idle, received message :[$msg]")
      msg match {
        case LoadScript(packageId, observingMode, replyTo) =>
          val sequencerServer    = sequencerServerFactory.make(packageId, observingMode, Some(sequenceComponentName))
          val registrationResult = sequencerServer.start()
          replyTo ! LoadScriptResponse(registrationResult)
          registrationResult match {
            case Right(value) =>
              log.info(s"Successfully started sequencer with sequencer id :$packageId in observation mode: $observingMode")
              running(sequencerServer, value)
            case Left(value) =>
              log.error(s"Failed to start sequencer: ${value.msg}")
              Behaviors.same
          }
        case GetStatus(replyTo) =>
          replyTo ! GetStatusResponse(None)
          Behaviors.same
        case UnloadScript(replyTo) =>
          replyTo ! Done
          Behaviors.same
        case Stop => Behaviors.stopped
      }
    }

    def running(sequencerServer: SequencerServer, location: AkkaLocation): Behavior[SequenceComponentMsg] =
      Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
        import ctx.executionContext
        log.debug(s"Sequence Component in lifecycle state :Running, received message :[$msg]")
        msg match {
          case UnloadScript(replyTo) =>
            log.info(s"Unloaded script successfully")
            sequencerServer.shutDown().foreach(_ => replyTo ! Done)
            idle
          case GetStatus(replyTo) =>
            replyTo ! GetStatusResponse(Some(location))
            Behaviors.same
          case LoadScript(_, _, replyTo) =>
            replyTo ! LoadScriptResponse(Left(LoadScriptError("Loading script failed: Sequencer already running")))
            Behaviors.same
          case Stop => Behaviors.same
        }
      }
    idle
  }
}
