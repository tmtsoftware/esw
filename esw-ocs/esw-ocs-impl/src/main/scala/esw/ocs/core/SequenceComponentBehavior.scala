package esw.ocs.core

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.models.AkkaLocation
import esw.ocs.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, Stop, UnloadScript}
import esw.ocs.api.models.messages.SequenceComponentResponses.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.models.messages.{RegistrationError, SequenceComponentMsg}
import esw.ocs.internal.SequencerWiring

object SequenceComponentBehavior {

  def behavior(sequenceComponentName: String): Behavior[SequenceComponentMsg] = {

    lazy val idle: Behavior[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] {
      case LoadScript(sequencerId, observingMode, replyTo) =>
        val wiring             = new SequencerWiring(sequencerId, observingMode, Some(sequenceComponentName))
        val registrationResult = wiring.start()
        replyTo ! LoadScriptResponse(registrationResult)
        registrationResult.map(x => running(wiring, x)).getOrElse(Behaviors.same)
      case GetStatus(replyTo) =>
        replyTo ! GetStatusResponse(None)
        Behaviors.same
      case UnloadScript(replyTo) =>
        replyTo ! Done
        Behaviors.same
      case Stop => Behaviors.stopped
    }

    def running(wiring: SequencerWiring, location: AkkaLocation): Behavior[SequenceComponentMsg] =
      Behaviors.receive[SequenceComponentMsg] { (ctx, msg) =>
        import ctx.executionContext

        msg match {
          case UnloadScript(replyTo) =>
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
