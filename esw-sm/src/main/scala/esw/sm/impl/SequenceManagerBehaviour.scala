package esw.sm.impl

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.sm.api.Response.{Error, Ok}
import esw.sm.api.SequenceManagerMsg._
import esw.sm.api.{RichSequence, SequenceManagerMsg, SequenceStatus}

object SequenceManagerBehaviour {
  var sequences: List[RichSequence] = List.empty

  def behaviour(locationService: LocationServiceUtil): Behavior[SequenceManagerMsg] =
    Behaviors.receiveMessage[SequenceManagerMsg] { msg =>
      msg match {
        case Shutdown(replyTo) =>
          //unregister from location service
          //http service shutdown and unbind port
          //maybe terminate actor system
          replyTo ! Ok
        case AcceptSequence(sequence, replyTo) =>
          sequences = sequences :+ RichSequence(sequence)
          replyTo ! Ok
        case ValidateSequence(sequence, replyTo) => {
          //handleValidate
        }
        case StartSequence(runId, replyTo) =>
          val sequence: Option[RichSequence] = sequences.find(_.sequence.runId == runId)
          sequence match {
            case Some(sequence) =>
              //get resource names and check if available or not, check for conflict and
              //pass sequence to Top level sequencer and change the status accordingly}
              sequence.updateStatus(SequenceStatus.InFlight)
              replyTo ! Ok
            case None => replyTo ! Error(s"sequence with runId $runId does not exist")
          }
        case ListSequence(replyTo)                               => replyTo ! sequences
        case GetSequence(runId, replyTo)                         => replyTo ! sequences.find(_.sequence.runId == runId)
        case StartSequencer(sequencerId, observingMode, replyTo) =>
          //find available sequence component with given subsystem
          // sequenceComponent ! LoadScript(sequencerId, observingMode)
          replyTo ! Ok
        case ShutdownSequencer(sequencerId, observingMode, replyTo) =>
          // locationUtils.resolve(sequencerId, observingMode) ==> sequencer
          // get sequence component name from sequencer name
          // sequenceComponent ! UnloadScript
          replyTo ! Ok
        case GoOnlineSequencer(sequencerId, observingMode, replyTo) =>
          // locationUtils.resolve(sequencerId, observingMode) ==> sequencer
          // sequencer ! GoOnline
          replyTo ! Ok
        case GoOfflineSequencer(sequencerId, observingMode, replyTo) =>
          // locationUtils.resolve(sequencerId, observingMode) ==> sequencer
          // sequencer ! GoOffline
          replyTo ! Ok
      }

      Behaviors.same
    }
}
