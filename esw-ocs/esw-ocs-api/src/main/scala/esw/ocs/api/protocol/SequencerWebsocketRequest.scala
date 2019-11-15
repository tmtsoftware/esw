package esw.ocs.api.protocol

import csw.params.core.models.Id

sealed trait SequencerWebsocketRequest

private[ocs] object SequencerWebsocketRequest {
  case class QueryFinal(sequenceId: Id) extends SequencerWebsocketRequest
}
