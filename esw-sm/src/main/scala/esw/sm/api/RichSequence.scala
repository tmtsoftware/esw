package esw.sm.api

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence

sealed trait SequenceStatus extends Product with Serializable

object SequenceStatus {
  case object Pending   extends SequenceStatus
  case object InFlight  extends SequenceStatus
  sealed trait Finished extends SequenceStatus
  object Finished {
    case class Success(submitResponse: SubmitResponse) extends Finished
    case class Failure(submitResponse: SubmitResponse) extends Finished
  }
}

case class RichSequence(sequence: Sequence, status: SequenceStatus) {
  def updateStatus(_status: SequenceStatus): RichSequence = copy(status = _status)
}

object RichSequence {
  def apply(sequence: Sequence): RichSequence = new RichSequence(sequence, SequenceStatus.Pending)
}
