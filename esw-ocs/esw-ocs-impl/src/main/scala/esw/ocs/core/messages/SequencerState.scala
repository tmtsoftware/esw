package esw.ocs.core.messages

import csw.command.client.messages.sequencer.SequencerMsg
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}
import esw.ocs.core.messages.SequencerMessages.{
  AbortSequenceMessage,
  GoingOfflineMessage,
  GoingOnlineMessage,
  IdleMessage,
  InProgressMessage,
  OfflineMessage,
  SequenceLoadedMessage,
  ShuttingDownMessage
}

import scala.collection.immutable.IndexedSeq

sealed abstract class SequencerState[+T <: SequencerMsg] extends EnumEntry with Lowercase
object SequencerState extends Enum[SequencerState[SequencerMsg]] {

  def values: IndexedSeq[SequencerState[SequencerMsg]] = findValues

  case object Idle             extends SequencerState[IdleMessage]
  case object Loaded           extends SequencerState[SequenceLoadedMessage]
  case object InProgress       extends SequencerState[InProgressMessage]
  case object Offline          extends SequencerState[OfflineMessage]
  case object GoingOnline      extends SequencerState[GoingOnlineMessage]
  case object GoingOffline     extends SequencerState[GoingOfflineMessage]
  case object ShuttingDown     extends SequencerState[ShuttingDownMessage]
  case object AbortingSequence extends SequencerState[AbortSequenceMessage]
}
