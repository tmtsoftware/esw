package esw.ocs.api.actor.messages

import csw.command.client.messages.sequencer.SequencerMsg
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.ExternalSequencerState

sealed trait SequencerState[+T <: SequencerMsg] extends OcsAkkaSerializable {
  def toExternal: ExternalSequencerState = {
    import SequencerState._
    this match {
      case _: Idle.type    => ExternalSequencerState.Idle
      case _: Loaded.type  => ExternalSequencerState.Loaded
      case _: Running.type => ExternalSequencerState.Running
      case _: Offline.type => ExternalSequencerState.Offline
      case _               => ExternalSequencerState.Processing
    }
  }
}

object SequencerState {
  case object Idle             extends SequencerState[IdleMessage]
  case object Loaded           extends SequencerState[SequenceLoadedMessage]
  case object Running          extends SequencerState[RunningMessage]
  case object Offline          extends SequencerState[OfflineMessage]
  case object GoingOnline      extends SequencerState[GoingOnlineMessage]
  case object GoingOffline     extends SequencerState[GoingOfflineMessage]
  case object AbortingSequence extends SequencerState[AbortSequenceMessage]
  case object Stopping         extends SequencerState[StopMessage]
  case object Submitting       extends SequencerState[SubmitMessage]
  case object Starting         extends SequencerState[StartingMessage]
}
