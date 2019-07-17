package esw.ocs.framework.core

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.model.scaladsl.AkkaLocation
import esw.ocs.framework.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}
import esw.ocs.framework.api.models.messages.{LoadScriptError, SequenceComponentMsg}
import esw.ocs.framework.internal.SequencerWiring

object SequenceComponentBehavior {

  def behavior: Behavior[SequenceComponentMsg] = {

    lazy val idle: Behavior[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] {
      case LoadScript(sequencerId, observingMode, replyTo) =>
        val wiring             = new SequencerWiring(sequencerId, observingMode)
        val loadScriptResponse = wiring.start()
        replyTo ! loadScriptResponse
        loadScriptResponse.map(x => running(wiring, x)).getOrElse(Behaviors.same)
      case GetStatus(replyTo) =>
        replyTo ! None
        Behaviors.same
      case UnloadScript(replyTo) =>
        replyTo ! Done
        Behaviors.same
    }

    def running(wiring: SequencerWiring, location: AkkaLocation): Behavior[SequenceComponentMsg] =
      Behaviors.receiveMessage[SequenceComponentMsg] {
        case UnloadScript(replyTo) =>
          wiring.shutDown()
          replyTo ! Done
          idle
        case GetStatus(replyTo) =>
          replyTo ! Some(location)
          Behaviors.same
        case LoadScript(_, _, replyTo) =>
          replyTo ! Left(LoadScriptError(new Throwable("Sequencer already running")))
          Behaviors.same
      }
    idle
  }
}
