package esw.sm.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import esw.ocs.api.{SequencerCommandFactoryApi, protocol}
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import esw.sm.api.Response.{Error, Ok}
import esw.sm.api.SequenceManagerMsg._
import esw.sm.api.{RichSequence, SequenceManagerMsg, SequenceStatus}

object SequenceManagerBehaviour {
  var sequences: List[RichSequence] = List.empty

  def behaviour(
      locationService: LocationServiceUtil,
      sequencerCommandFactory: SequencerCommandFactoryApi
  )(implicit actorSystem: ActorSystem[SpawnProtocol.Command]): Behavior[SequenceManagerMsg] =
    Behaviors.receiveMessage[SequenceManagerMsg] { msg =>
      import actorSystem.executionContext
      msg match {
        case Shutdown(replyTo) =>
          //unregister from location service
          //http service shutdown and unbind port
          //maybe terminate actor system
          replyTo ! Ok
        case AcceptSequence(sequence, replyTo) =>
          sequences = sequences :+ RichSequence(sequence)
          replyTo ! Ok
        case ValidateSequence(sequence, replyTo) =>
        //handleValidate
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
        case ListSequence(replyTo)                             => replyTo ! sequences
        case GetSequence(runId, replyTo)                       => replyTo ! sequences.find(_.sequence.runId == runId)
        case StartSequencer(packageId, observingMode, replyTo) =>
          //find available sequence component with given subsystem
          // sequenceComponent ! LoadScript(packageId, observingMode)
          replyTo ! Ok
        case ShutdownSequencer(packageId, observingMode, replyTo) =>
          // locationUtils.resolve(packageId, observingMode) ==> sequencer
          // get sequence component name from sequencer name
          // sequenceComponent ! UnloadScript
          replyTo ! Ok
        case GoOnlineSequencer(packageId, observingMode, replyTo) =>
          sequencerCommandFactory.make(packageId, observingMode).map { sequencerCommandApi =>
            sequencerCommandApi.goOnline().foreach {
              case protocol.Ok => replyTo ! Ok
              case response    => replyTo ! Error(s"failed with error ${response.toString}")
            }
          }
        case GoOfflineSequencer(packageId, observingMode, replyTo) =>
          sequencerCommandFactory.make(packageId, observingMode).map { sequencerCommandApi =>
            sequencerCommandApi.goOffline().foreach {
              case protocol.Ok => replyTo ! Ok
              case response    => replyTo ! Error(s"failed with error ${response.toString}")
            }
          }
      }

      Behaviors.same
    }
}
