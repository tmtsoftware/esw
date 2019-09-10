package esw.ocs.impl.messages

import csw.command.client.messages.sequencer.SequencerMsg
import enumeratum.{Enum, EnumEntry}
import esw.ocs.api.models.codecs.OcsAkkaSerializable
import esw.ocs.impl.messages.SequencerMessages._

import scala.collection.immutable.IndexedSeq

sealed trait SequencerState[+T <: SequencerMsg] extends EnumEntry with OcsAkkaSerializable
object SequencerState extends Enum[SequencerState[SequencerMsg]] {

  override def values: IndexedSeq[SequencerState[SequencerMsg]] = findValues

  case object Idle             extends SequencerState[IdleMessage]
  case object Loaded           extends SequencerState[SequenceLoadedMessage]
  case object InProgress       extends SequencerState[InProgressMessage]
  case object Offline          extends SequencerState[OfflineMessage]
  case object GoingOnline      extends SequencerState[GoingOnlineMessage]
  case object GoingOffline     extends SequencerState[GoingOfflineMessage]
  case object ShuttingDown     extends SequencerState[ShuttingDownMessage]
  case object AbortingSequence extends SequencerState[AbortSequenceMessage]
}
