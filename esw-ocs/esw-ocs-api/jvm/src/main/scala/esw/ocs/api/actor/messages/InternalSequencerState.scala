package esw.ocs.api.actor.messages

import csw.command.client.messages.sequencer.SequencerMsg
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.SequencerState

sealed trait InternalSequencerState[+T <: SequencerMsg] extends OcsAkkaSerializable {

  def name: String = this.getClass.getSimpleName.dropRight(1) // remove $ from class name

  def toExternal: SequencerState = {
    import InternalSequencerState._
    this match {
      case _: Idle.type    => SequencerState.Idle
      case _: Loaded.type  => SequencerState.Loaded
      case _: Running.type => SequencerState.Running
      case _: Offline.type => SequencerState.Offline
      case _               => SequencerState.Processing
    }
  }
}

object InternalSequencerState {
  case object Idle             extends InternalSequencerState[IdleMessage]
  case object Loaded           extends InternalSequencerState[SequenceLoadedMessage]
  case object Running          extends InternalSequencerState[RunningMessage]
  case object Offline          extends InternalSequencerState[OfflineMessage]
  case object GoingOnline      extends InternalSequencerState[GoingOnlineMessage]
  case object GoingOffline     extends InternalSequencerState[GoingOfflineMessage]
  case object AbortingSequence extends InternalSequencerState[AbortSequenceMessage]
  case object Stopping         extends InternalSequencerState[StopMessage]
  case object Submitting       extends InternalSequencerState[SubmitMessage]
  case object Starting         extends InternalSequencerState[StartingMessage]
}
