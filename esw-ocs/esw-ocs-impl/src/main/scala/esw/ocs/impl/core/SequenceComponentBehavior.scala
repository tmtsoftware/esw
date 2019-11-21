package esw.ocs.impl.core

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import esw.ocs.api.protocol.{GetStatusResponse, ScriptError, ScriptResponse}
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg._

object SequenceComponentBehavior {

  def behavior(
      sequenceComponentName: String,
      log: Logger,
      sequencerServerFactory: SequencerServerFactory
  ): Behavior[SequenceComponentMsg] = {

    def load(packageId: String, observingMode: String, replyTo: ActorRef[ScriptResponse]): Behavior[SequenceComponentMsg] = {
      val sequencerServer    = sequencerServerFactory.make(packageId, observingMode, Some(sequenceComponentName))
      val registrationResult = sequencerServer.start()
      replyTo ! ScriptResponse(registrationResult)
      registrationResult match {
        case Right(location) =>
          log.info(s"Successfully started sequencer with sequencer id :$packageId in observation mode: $observingMode")
          running(packageId, observingMode, sequencerServer, location)
        case Left(scriptError) =>
          log.error(s"Failed to start sequencer: ${scriptError.msg}")
          Behaviors.same
      }
    }

    lazy val idle: Behavior[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] { msg =>
      log.debug(s"Sequence Component in lifecycle state :Idle, received message :[$msg]")
      msg match {
        case LoadScript(packageId, observingMode, replyTo) =>
          load(packageId, observingMode, replyTo)
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

    def running(
        packageId: String,
        observingMode: String,
        sequencerServer: SequencerServer,
        location: AkkaLocation
    ): Behavior[SequenceComponentMsg] =
      Behaviors.receiveMessage[SequenceComponentMsg] { msg =>
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
            load(packageId, observingMode, replyTo)
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
