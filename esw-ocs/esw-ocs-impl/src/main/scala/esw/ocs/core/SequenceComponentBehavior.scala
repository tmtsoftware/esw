package esw.ocs.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import esw.ocs.api.models.responses.SequenceComponentResponse.{Done, GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.models.responses.RegistrationError
import esw.ocs.client.messages.SequenceComponentMsg
import esw.ocs.client.messages.SequenceComponentMsg.{GetStatus, LoadScript, Stop, UnloadScript}
import esw.ocs.internal.SequencerWiring

object SequenceComponentBehavior {

  def behavior(
      sequenceComponentName: String,
      log: Logger,
      sequencerWiring: (String, String, Option[String]) => SequencerWiring
  ): Behavior[SequenceComponentMsg] = {

    lazy val idle: Behavior[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] { msg =>
      log.debug(s"Sequence Component in lifecycle state :Idle, received message :[$msg]")
      msg match {
        case LoadScript(sequencerId, observingMode, replyTo) =>
          val wiring             = sequencerWiring(sequencerId, observingMode, Some(sequenceComponentName))
          val registrationResult = wiring.start()
          replyTo ! LoadScriptResponse(registrationResult)
          registrationResult match {
            case Right(value) =>
              log.info(s"Successfully started sequencer with sequencer id :$sequencerId in observation mode: $observingMode")
              running(wiring, value)
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

    def running(wiring: SequencerWiring, location: AkkaLocation): Behavior[SequenceComponentMsg] =
      Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
        import ctx.executionContext
        log.debug(s"Sequence Component in lifecycle state :Running, received message :[$msg]")
        msg match {
          case UnloadScript(replyTo) =>
            log.info(s"Unloaded script successfully")
            wiring.shutDown().foreach(_ => replyTo ! Done)
            idle
          case GetStatus(replyTo) =>
            replyTo ! GetStatusResponse(Some(location))
            Behaviors.same
          case LoadScript(_, _, replyTo) =>
            replyTo ! LoadScriptResponse(Left(RegistrationError("Loading script failed: Sequencer already running")))
            Behaviors.same
          case Stop => Behaviors.same
        }
      }
    idle
  }
}
